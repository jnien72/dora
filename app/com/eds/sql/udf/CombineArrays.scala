package com.eds.sql.udf

import org.apache.spark.sql.api.java.UDF3
import org.apache.spark.sql.types.{DataType, DataTypes}

class CombineArrays extends UDF3[Seq[String], Seq[String], String, Seq[String]] with DoraUDF {

  override def call(array1: Seq[String], array2: Seq[String], delimiter: String): Seq[String] = {
    if(array1.length != array2.length) {
      throw new RuntimeException("Two array Should have same length")
    }

    array1.zipWithIndex.map(e1 => {
      e1._1 + delimiter + array2(e1._2)
    })
  }

  override def getUDFType:Class[_]= classOf[UDF3[Seq[String], Seq[String], String, Seq[String]]]

  override def getUsage():String = {
    this.getClass.getSimpleName.charAt(0).toLower+this.getClass.getSimpleName.substring(1) +"(array, array, delimiter)"
  }

  override def getDescription: String = {
    "Combine two array by element with specified delimiter and return new array"
  }

  override def getDataType: DataType = DataTypes.createArrayType(DataTypes.StringType)
}
