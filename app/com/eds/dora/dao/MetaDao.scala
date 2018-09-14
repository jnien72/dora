package com.eds.dora.dao

import com.eds.dora.cluster.util.InstanceUtils
import com.eds.dora.util.{ZkClient, ZkConstants}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

object MetaDao {

  val LOG = LoggerFactory.getLogger(getClass)

  def list(table: MetaTable.Key): List[(String, String)] = {
    val result = ArrayBuffer[(String, String)]()
    val zkPath=ZkConstants.ZK_NAMESPACE_HOME+"/"+InstanceUtils.getNamespace()+"/metadata/"+table.toString
    val keys=ZkClient.get().getChildren(zkPath).toArray.toList.map(x=>x.toString)
    keys.foreach(key=>{
      val value=get(table,key).getOrElse(null)
      result += ((key, value))
    })
    result.toList
  }

  def get(table: MetaTable.Key, key: String): Option[String] = {
    try{
      val zkPath=ZkConstants.ZK_NAMESPACE_HOME+"/"+InstanceUtils.getNamespace()+"/metadata/"+table.toString+"/"+key
      Option(ZkClient.get().readData[String](zkPath))
    }catch{
      case ex:Exception=>Option(null)
    }
  }

  def set(table: MetaTable.Key, key: String, value: String): Unit = {
    try{
      val zkPath=ZkConstants.ZK_NAMESPACE_HOME+"/"+InstanceUtils.getNamespace()+"/metadata/"+table.toString+"/"+key
      if(ZkClient.get().exists(zkPath)){
        ZkClient.get().writeData(zkPath,value)
      }else{
        ZkClient.get().createPersistent(zkPath,value)
      }
    }catch{
      case ex:Exception=>
    }
  }

  def delete(table: MetaTable.Key, key: String): Unit = {
    try{
      val zkPath=ZkConstants.ZK_NAMESPACE_HOME+"/"+InstanceUtils.getNamespace()+"/metadata/"+table.toString+"/"+key
      ZkClient.get().deleteRecursive(zkPath)
    }catch{
      case ex:Exception=>
    }
  }
}
