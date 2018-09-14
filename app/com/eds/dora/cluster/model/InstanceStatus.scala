package com.eds.dora.cluster.model

case class InstanceStatus(var startTime:Long, var hostname:String, var servicePort:Int, var sparkUiPort:Int)
