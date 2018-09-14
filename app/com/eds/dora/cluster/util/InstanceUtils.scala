package com.eds.dora.cluster.util

import com.eds.dora.cluster.model.{InstanceInfo, InstanceStatus, NamespaceInfo}
import com.eds.dora.util._

object InstanceUtils {

  //--------------------------------------------------
  // the following methods only works inside instance
  //--------------------------------------------------

  def getNamespace():String={
    if(System.getProperty("instance.id")==null){
      throw new RuntimeException("Unable to retrieve data")
    }
    System.getProperty("instance.id").split("-")(0)
  }

  def getInstanceName():String={
    val instanceId=System.getProperty("instance.id")
    val tokens=instanceId.split("-")
    tokens(0)+"-"+tokens(1)
  }

  def getServicePort():Int={
    if(System.getProperty("service.port")==null){
      throw new RuntimeException("Unable to retrieve data")
    }
    System.getProperty("service.port").toInt
  }
  def getSparkUiPort():Int={
    if(System.getProperty("spark.ui.port")==null){
      throw new RuntimeException("Unable to retrieve data")
    }
    System.getProperty("spark.ui.port").toInt
  }

  def getInstanceConfiguration():Map[String,String]={
    val instanceName=getInstanceName()
    val json=ZkClient.get().readData[String](ZkConstants.ZK_INSTANCE_HOME+"/"+instanceName)
    val instanceInfo=JsonUtils.fromJson[InstanceInfo](json)
    instanceInfo.configuration
  }

  def getNamespaceInfo(): NamespaceInfo = {
    JsonUtils.fromJson[NamespaceInfo](
      ZkClient.get().readData[String](
        ZkConstants.ZK_NAMESPACE_HOME.toString + "/" + InstanceUtils.getNamespace()
      )
    )
  }
  //--------------------------------------------------


  def getHeapSizeInMbFromExpression(heapSizeExp:String):Int={
    var heapCapacityInMB:Int=heapSizeExp.replace("g","").replace("m","").toInt
    if(heapSizeExp.endsWith("g")){
      heapCapacityInMB=heapCapacityInMB*1024
    }
    heapCapacityInMB
  }

  def getInstanceStatus(instanceName:String):InstanceStatus={
    val statusStr=ZkClient.get().readData[String](ZkConstants.ZK_INSTANCE_HOME+"/"+instanceName+"/status")
    JsonUtils.fromJson[InstanceStatus](statusStr)
  }

  //outputs: id->(info,status)
  def retrieveAllInstanceDetails():Map[String,(InstanceInfo,InstanceStatus)]={
    val result=ZkClient.get().getChildren(ZkConstants.ZK_INSTANCE_HOME.toString).toArray.map(x=>{
      val instanceName=x.toString
      val instanceZkPath=ZkConstants.ZK_INSTANCE_HOME.toString+"/"+instanceName
      val instanceData=ZkClient.get().readData[String](instanceZkPath)
      val instanceInfo=JsonUtils.fromJson[InstanceInfo](instanceData)
      val statusZkPath=instanceZkPath+"/status"
      if(!ZkClient.get().exists(statusZkPath)){
        (instanceInfo.getInstanceId(),(instanceInfo,null))
      }else{
        val statusData=ZkClient.get().readData[String](statusZkPath)
        val instanceStatus=JsonUtils.fromJson[InstanceStatus](statusData)
        (instanceInfo.getInstanceId(),(instanceInfo,instanceStatus))
      }
    })
    result.toMap
  }
}
