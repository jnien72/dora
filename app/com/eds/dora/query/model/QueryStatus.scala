package com.eds.dora.query.model

import com.eds.dora.util.JsonUtils

class QueryStatus extends Serializable{
  var jobId: String=""
  var user: String=""
  var sql: String=""
  var startMillis:String=""
  var elapsed:String=""
  var status:String=""

  def preTouch():Any={
    JsonUtils.fromJson[QueryStatus](JsonUtils.toJson(new QueryStatus()))
  }
}
