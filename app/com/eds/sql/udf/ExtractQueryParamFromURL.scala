package com.eds.sql.udf

import org.apache.spark.sql.api.java.UDF2
import org.apache.spark.sql.types.{DataType, DataTypes}

class ExtractQueryParamFromURL extends UDF2[String, String, String] with DoraUDF {

  override def call(url: String, parameter: String): String = {
    try {
      val queryParamsStartIdx = url.indexOf("?")
      val parameters = url.substring(queryParamsStartIdx + 1)
      val filteredArrays = parameters.split("&").map(x => x.split("=")).filter(x => x(0).equals(parameter))
      filteredArrays(filteredArrays.length - 1)(1)
    }catch{
      case _:Throwable=>null
    }
  }

  override def getDataType(): DataType = DataTypes.StringType

  override def getUDFType():Class[_]= classOf[UDF2[String,String,String]]

  override def getUsage():String = {
    this.getClass.getSimpleName.charAt(0).toLower+this.getClass.getSimpleName.substring(1) +"(url,paramKey)"
  }

  override def getDescription = {
    "Specify a parameter key to be matched against a URL, the parameter value will be returned as string"
  }
}