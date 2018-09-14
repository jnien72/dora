package com.eds.dora.util

import java.sql.Timestamp
import com.eds.sql.udf.DoraUDF
import org.apache.commons.logging.LogFactory
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem}
import org.apache.hadoop.hive.common.`type`.HiveDecimal
import org.apache.hadoop.hive.serde2.io.{DateWritable, TimestampWritable}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import org.reflections.Reflections

object HiveUtils {

  private val LOG = LogFactory.getLog(getClass)
  private var definedUdfs: Array[(String, Any, Class[_], DataType)] = null

  val fs = FileSystem.get(new Configuration())

  //returns (1)udfName, (2)instance, (3)instance class as sql UDF, (4) dataType
  def getDefinedUdfs(): Array[(String, Any, Class[_], DataType)] = {
    "getDefinedUdfs".synchronized({
      if (definedUdfs == null) {
        val reflections = new Reflections(classOf[DoraUDF].getPackage.getName)
        val udfInstances = reflections.getSubTypesOf(classOf[DoraUDF]).toArray.map(x => x.asInstanceOf[Class[_]].newInstance())
        definedUdfs = udfInstances.map(udfInstance => {
          val className = udfInstance.getClass.getSimpleName
          val functionName = className.substring(0, 1).toLowerCase() + className.substring(1)
          val doraUdf = udfInstance.asInstanceOf[DoraUDF]
          val dataType = doraUdf.getDataType
          val udfType = doraUdf.getUDFType
          (functionName, udfInstance, udfType, dataType)
        })
      }
      definedUdfs
    })
  }

  protected val primitiveTypes =
    Seq(StringType, IntegerType, LongType, DoubleType, FloatType, BooleanType, ByteType,
      ShortType, DateType, TimestampType, BinaryType)

  def toHiveString(x: (Any, DataType)): String = x match {
    case (struct: Row, StructType(fields)) =>
      struct.toSeq.zip(fields).map {
        case (v, t) => s""""${t.name}":${toHiveStructString(v, t.dataType)}"""
      }.mkString("{", ",", "}")
    case (seq: Seq[_], ArrayType(typ, _)) =>
      seq.map(v => (v, typ)).map(toHiveStructString).mkString("[", ",", "]")
    case (map: Map[_, _], MapType(kType, vType, _)) =>
      map.map {
        case (key, value) =>
          toHiveStructString((key, kType)) + ":" + toHiveStructString((value, vType))
      }.toSeq.sorted.mkString("{", ",", "}")
    case (null, _) => "NULL"
    case (d: Int, DateType) => new DateWritable(d).toString
    case (t: Timestamp, TimestampType) => new TimestampWritable(t).toString
    case (bin: Array[Byte], BinaryType) => new String(bin, "UTF-8")
    case (decimal: java.math.BigDecimal, DecimalType()) =>
      // Hive strips trailing zeros so use its toString
      HiveDecimal.create(decimal).toString
    case (other, tpe) if primitiveTypes contains tpe => other.toString
  }

  protected def toHiveStructString(a: (Any, DataType)): String = a match {
    case (struct: Row, StructType(fields)) =>
      struct.toSeq.zip(fields).map {
        case (v, t) => s""""${t.name}":${toHiveStructString(v, t.dataType)}"""
      }.mkString("{", ",", "}")
    case (seq: Seq[_], ArrayType(typ, _)) =>
      seq.map(v => (v, typ)).map(toHiveStructString).mkString("[", ",", "]")
    case (map: Map[_, _], MapType(kType, vType, _)) =>
      map.map {
        case (key, value) =>
          toHiveStructString((key, kType)) + ":" + toHiveStructString((value, vType))
      }.toSeq.sorted.mkString("{", ",", "}")
    case (null, _) => "null"
    case (s: String, StringType) => "\"" + s + "\""
    case (decimal, DecimalType()) => decimal.toString
    case (other, tpe) if primitiveTypes contains tpe => other.toString
  }

  def validateQueryCmd(sql: String): Unit = {
    if (false == EnvProperty.get(EnvConstants.QUERY_WRITE_PERMISSION).toBoolean) {
      var lowerCasedSql = sql.toLowerCase().trim.replace("\n", " ").replace("\t", " ")
      if (lowerCasedSql.startsWith("with")) {
        var current = lowerCasedSql.indexOf("(");
        var end = lowerCasedSql.length - 1
        var balance = 0
        var newStart = end
        var lastChar = ','
        var currentQuote: Int = 0
        while (current <= end && current < lowerCasedSql.length) {
          if (currentQuote == 0 && (lowerCasedSql.charAt(current).equals(''') || lowerCasedSql.charAt(current).equals('"'))) {
            currentQuote = lowerCasedSql.charAt(current).toInt
          } else if (currentQuote != 0) {
            if (lowerCasedSql.charAt(current).toInt == currentQuote) {
              currentQuote = 0
            }
          } else {
            if (lowerCasedSql.charAt(current) != (' ') ||
              lowerCasedSql.charAt(current) != ('\t') ||
              lowerCasedSql.charAt(current) != ('\n')) {
              if (lowerCasedSql.charAt(current) == ('(')) {
                balance += 1
              } else if (lowerCasedSql.charAt(current) == (')')) {
                balance -= 1
              } else if (balance == 0) {
                if (lowerCasedSql.charAt(current) == ',') {
                  current = lowerCasedSql.indexOf('(', current + 1)
                } else {
                  end = (-1)
                  newStart = current + 1
                }
              }
            }
          }
          current += 1
        }
        lowerCasedSql = lowerCasedSql.substring(newStart).trim
      }

      if (lowerCasedSql.length > 1) {
        if (!lowerCasedSql.startsWith("select")
          && !lowerCasedSql.startsWith("show")
          && !lowerCasedSql.startsWith("describe")
          && !lowerCasedSql.startsWith("refresh")
          && !lowerCasedSql.startsWith("set")
          && !lowerCasedSql.startsWith("explain")
        ) {
          throw new UnsupportedOperationException(sql)
        }
      }
    }
  }
}