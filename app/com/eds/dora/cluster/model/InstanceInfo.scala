package com.eds.dora.cluster.model

import com.eds.dora.cluster.util.InstanceUtils
import com.eds.dora.util.{JsonUtils, Md5Utils}
import com.fasterxml.jackson.annotation.JsonIgnore

class InstanceInfo {

  var namespace:String = null
  var instanceType: String = null
  var heapSize: String = null
  var configuration : scala.collection.immutable.SortedMap[String, String] = null
  var enabled:Boolean = true

  @JsonIgnore
  def getInstanceName():String={
    namespace+"-"+instanceType
  }

  @JsonIgnore
  def getInstanceId():String={
    getInstanceName()+"-"+Md5Utils.getMD5(JsonUtils.toJson(this)).substring(0,8)
  }

  @JsonIgnore
  def getInstanceHeapSizeInMB():Int={
    InstanceUtils.getHeapSizeInMbFromExpression(heapSize)
  }
}
