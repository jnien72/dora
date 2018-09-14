package com.eds.dora.util

import java.util.concurrent.ConcurrentHashMap

object GlobalSync {

  val map=new ConcurrentHashMap[String, Object]()

  def get(key:String):Object={
    this.synchronized{
      var obj=map.get(key)
      if(obj==null){
        obj=new Object()
        map.put(key,obj)
      }
      obj
    }
  }
}
