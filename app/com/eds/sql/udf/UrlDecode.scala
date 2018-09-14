package com.eds.sql.udf

import jodd.util.URLDecoder
import org.apache.spark.sql.api.java.UDF1
import org.apache.spark.sql.types.{DataType, DataTypes}

class UrlDecode extends UDF1[String, String] with DoraUDF {

  override def call(str: String): String = {
    try {
      URLDecoder.decode(str, "UTF-8")
    } catch {
      case _ => {
        str
      }
    }
  }

  override def getUDFType:Class[_]= classOf[UDF1[String, String]]

  override def getUsage():String = {
    this.getClass.getSimpleName.charAt(0).toLower+this.getClass.getSimpleName.substring(1) +"(string)"
  }

  override def getDescription: String = {
    "Decode encoded url string"
  }

  override def getDataType: DataType = DataTypes.StringType
}
