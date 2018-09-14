package com.eds.dora.dao

object MetaTable extends Enumeration {
  type Key = Value
  val CONNECTION = Value("ds_connection")
  val QUERY_TEMPLATE = Value("query_template")
  val DATASOURCE = Value("ds_entity")
  val ETL = Value("etl_entity")
  val ETL_GROUP = Value("etl_group_entity")
}
