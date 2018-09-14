package com.eds.sql.udf

import org.apache.spark.sql.api.java.UDF2
import org.apache.spark.sql.types.{DataType, DataTypes}

class ArrayEquals extends UDF2[Seq[String], Seq[String], Boolean] with DoraUDF {

  override def call(array: Seq[String], compareArray: Seq[String]): Boolean = {
    if(array != null && compareArray != null) {
      array.toSet[String] == compareArray.toSet[String]
    } else {
      false
    }
  }

  override def getUDFType:Class[_]= classOf[UDF2[Seq[String], Seq[String], Boolean]]

  override def getUsage():String = {
    this.getClass.getSimpleName.charAt(0).toLower+this.getClass.getSimpleName.substring(1) +"(array, compareArray)"
  }

  override def getDescription: String = {
    "Convert two arrays to set, compare them and return whether they contain same values"
  }

  override def getDataType: DataType = DataTypes.BooleanType
}
