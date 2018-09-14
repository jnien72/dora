package com.eds.sql.udf

import org.apache.spark.sql.api.java.UDF2
import org.apache.spark.sql.types.{DataType, DataTypes}

class MkString extends UDF2[Seq[String], String, String] with DoraUDF {

  override def call(array: Seq[String], delimiter: String): String = {
    array.mkString(delimiter)
  }

  override def getUDFType:Class[_]= classOf[UDF2[Seq[String], String, String]]

  override def getUsage():String = {
    this.getClass.getSimpleName.charAt(0).toLower+this.getClass.getSimpleName.substring(1) +"(array, delimiter)"
  }

  override def getDescription: String = {
    "concatenate array as a single string value with specified delimiter"
  }

  override def getDataType: DataType = DataTypes.StringType
}
