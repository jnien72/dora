package com.eds.dora.query.model


case class QueryResponse(elapsed:String, columns:Array[String], data:Array[Array[String]], error:String) extends Serializable
