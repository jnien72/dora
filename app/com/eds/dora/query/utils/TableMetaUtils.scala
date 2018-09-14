package com.eds.dora.query.utils

import com.eds.dora.cluster.util.InstanceUtils
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException
import org.apache.spark.sql.hive.HiveContext
import org.slf4j.LoggerFactory

object TableMetaUtils {

  private val LOG = LoggerFactory.getLogger(getClass)


  def getSchemas(): Array[String] = {
    Array[String](InstanceUtils.getNamespace())
  }

  def getTables(hiveContext: HiveContext): Array[String] = {
    val tableNames = hiveContext.tableNames()
    scala.util.Sorting.quickSort(tableNames)
    tableNames
  }

  def getFields(hiveContext: HiveContext, tableName: String): Array[String] = {
    try {
      retrieveFields(hiveContext.table(tableName), "").toArray
    } catch {
      case x: NoSuchTableException => {
        LOG.info(s"MetaData for table [${tableName}] is not existed")
        new Array[String](0)
      }
      case e: Throwable => {
        LOG.info(s"Exception occurred when query metaData for table [${tableName}]: " + e)
        new Array[String](0)
      }
    }

  }

  def retrieveFields(table: DataFrame, base: String): List[String] = {
    val fieldsAsArr = table.schema.toList
    val result = fieldsAsArr.flatMap(x => {
      if (x.dataType.typeName.equals("struct")) {
        val newBase = if (base.length == 0) x.name else base + "." + x.name
        retrieveFields(table.selectExpr(x.name + ".*"), newBase)
      } else {
        val fieldDescription = if (base.length == 0) x.name + " (" + x.dataType.typeName + ")"
        else base + "." + x.name + " (" + x.dataType.typeName + ")"
        List[String](fieldDescription)
      }
    })
    result
  }

}
