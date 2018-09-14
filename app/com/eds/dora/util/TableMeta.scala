package com.eds.dora.util

case class TableMeta(name: String, path: String, className: Option[String], group: Option[String], fields: Option[Array[String]], lastTxDate: Option[String], lastUpdate: Option[Long])