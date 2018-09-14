package com.eds.dora.util

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

object SqlUtils {

  val REPLACE_SYMBOL = "\\$@\\$" //Only to make sure not repeat with real table name
  val REPLACE_CHAR = '#'

  def replaceTableName(sql: String, originalTable: String, replaceTable: String):String = {
    val lowerCasedOriginalTable=originalTable.toLowerCase()
    val lowerCasedReplaceTable=replaceTable.toLowerCase()
    val tables = extractTablesFromQuery(sql)
    val matchedTable = tables.find(table => {
      table.equals(lowerCasedOriginalTable)
    })
    val parsedSql=removeComment(sql)

    if(matchedTable.isEmpty) {
      throw new RuntimeException(s"Sql '${parsedSql}' cannot contain table name ${lowerCasedOriginalTable}")
    }

    val splitSqlLines = parsedSql.split(System.lineSeparator())
    val replacedSqlLines = splitSqlLines.map(sql => {
      sql
        .replaceAll("(?i)(from\\040+`*)(" + lowerCasedOriginalTable + ")(`*\\040+|$)", """$1""" + lowerCasedReplaceTable + """$3""")
        .replaceAll("(?i)(join\\040+`*)(" + lowerCasedOriginalTable + ")(`*\\040+|$)", """$1""" + lowerCasedReplaceTable + """$3""")
    })
    replacedSqlLines.mkString(System.lineSeparator())
  }

  def extractMatchTablesFromQuery(sql: String, searchTables: Set[String]) : Set[String] = {
    val tables = extractTablesFromQuery(sql)
    tables.filter(table => {
      searchTables.contains(table)
    })
  }

  def extractTablesFromQuery(sql: String) : Set[String] = {
    val formatSql = getFormatSql(removeComment(sql.toLowerCase()))
    val subSqlIndexList = List[List[Int]](List[Int](0, formatSql.length)) ++ getAllSubSqlIndexs(formatSql)

    val tableSets: List[Set[String]] = subSqlIndexList.map(indexs => {
      val subSql = formatSql.substring(indexs(0), indexs(1))
      val replacedSql = replaceSubQueryWithUselessWord(subSql.toString, indexs, subSqlIndexList)
      val tables = extractRelatedTables(replacedSql)
      val withTableAliases = getTableAliasAfterWITH(formatSql)
      tables -- withTableAliases
    })
    var set: Set[String] = Set[String]()
    tableSets.foreach(tableSet => {
      set = set ++ tableSet
    })
    set
  }

  private def removeComment(sql: String): String = {
    val sqlList = sql.split(System.lineSeparator())
        .filter(str => {
          !str.startsWith("--")
        })
    sqlList.mkString(System.lineSeparator())
  }

  private def replaceSubQueryWithUselessWord(sql: String, indexs: List[Int], allSqlList: List[List[Int]]): String = {
    val newSql = new StringBuilder(sql)
    for(i <-  0 until allSqlList.size) {
      val thisIndex = allSqlList(i)
      if(!indexs.equals(thisIndex) && thisIndex(0) > indexs(0) && thisIndex(1) < indexs(1)) {
        var pos = thisIndex(0) - indexs(0)
        do {
          newSql(pos) = REPLACE_CHAR
          pos += 1
        } while (pos < thisIndex(1) - indexs(0))
      }
    }
    new Regex("\\040*(#+)\\040*").replaceAllIn(newSql.toString, " " + REPLACE_SYMBOL + " ")
  }

  private def extractRelatedTables(sql: String): Set[String] = {
    val fromTables = getTablesAfterFROM(sql)
    val joinTables = getTablesAfterJOIN(sql)
    val tables = fromTables ++ joinTables
    tables
  }

  private def getFormatSql(sql: String): String = {
    val replacedLineBreakSql = new Regex("[\\t\\n\\r]").replaceAllIn(sql," ")
    new Regex("\\040+").replaceAllIn(replacedLineBreakSql," ")
  }

  private def getTablesAfterFROM(sql: String): Set[String] = {
    val pattern = new Regex("(from)\\040+`*([\\w.]+)`*\\040*")
    pattern.findAllMatchIn(sql).map(m => {
      m.group(2)
    }).toSet[String]
  }

  private def getTablesAfterJOIN(sql: String): Set[String] = {
    val pattern = new Regex("(join)\\040+`*([\\w.]+)`*\\040*")
    pattern.findAllMatchIn(sql).map(m => {
      m.group(2)
    }).toSet[String]
  }

  private def getTableAliasAfterWITH(sql: String): Set[String] = {
    val patternFirstOne = new Regex("(with)\\040+`*([\\w.]+)`*\\040+(as|AS)")
    val patternAfterFirstOne = new Regex("\\),\\040?+`*([\\w.]+)`*\\040(as|AS)\\040?\\(")
    val matchedFirstOne = patternFirstOne.findAllMatchIn(sql).map(m => {
      m.group(2)
    }).toSet[String]
    val matchedAfterFirstOne = patternAfterFirstOne.findAllMatchIn(sql).map(m => {
      m.group(1)
    }).toSet[String]
    matchedFirstOne ++ matchedAfterFirstOne
  }

  private def getAllSubSqlIndexs(sql: String): List[List[Int]] = {
    val pattern = new Regex("(\\(\\040*SELECT)")
    val subQueryStartPosList = pattern.findAllMatchIn(sql).map(m => {
      m.start(1)
    }).toList

    val subQueryEndPosList = subQueryStartPosList.map(startPos => {
      getEndPosi(sql, startPos)
    })
    val list = ListBuffer[List[Int]]()
    for (i <- 0 until subQueryStartPosList.length) {
      list += List[Int](subQueryStartPosList(i), subQueryEndPosList(i))
    }
    list.toList
  }

  private def getEndPosi(sql: String, start: Int) : Int = {
    var index = start
    var countOfOpenParenthesis = 0
    var countOfCloseParenthesis = 0
    do {
      val ch = sql.charAt(index)
      if(ch.equals('(')) {
        countOfOpenParenthesis += 1
      }
      if(ch.equals(')')) {
        countOfCloseParenthesis += 1
      }
      index += 1
    } while((countOfOpenParenthesis != countOfCloseParenthesis) && (index < sql.length))
    index
  }
}
