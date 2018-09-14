package com.eds.dora.cluster.model

case class AgentInfo(var hostname:String,
                     var startTime:Long,
                     var heapCapacityInMB:Int,
                     var usedHeapInMB:Int,
                     var freeHeapInMB:Int)
