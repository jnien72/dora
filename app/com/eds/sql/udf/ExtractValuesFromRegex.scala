package com.eds.sql.udf

import java.util.regex.Pattern

import org.apache.spark.sql.api.java.UDF2
import org.apache.spark.sql.types.{DataType, DataTypes}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ExtractValuesFromRegex extends UDF2[String, String, Array[String]] with DoraUDF {
  var patternsMap: mutable.HashMap[String, Pattern] = new mutable.HashMap()
  override def call(inputText: String, expression: String): Array[String] = {
    val pattern = patternsMap.getOrElseUpdate(expression,Pattern.compile(expression))
    val tmp=new ArrayBuffer[String]()
    val matcher = pattern.matcher(inputText)
    while (matcher.find()) {
      tmp.append(matcher.group(0))
    }
    tmp.toArray
  }

  override def getDataType(): DataType = DataTypes.createArrayType(DataTypes.StringType)

  override def getUDFType():Class[_]= classOf[UDF2[String,String,Array[String]]]

  override def getUsage():String = {
    this.getClass.getSimpleName.charAt(0).toLower+this.getClass.getSimpleName.substring(1) +"(string,regexp)"
  }

  override def getDescription = {
    "Specify a regexp pattern to be matched against a string, the matched values will be returned as an array"
  }
}