package com.eds.dora.util

import java.util.concurrent.ConcurrentHashMap

import org.I0Itec.zkclient.serialize.ZkSerializer
import org.I0Itec.zkclient.{ZkClient => ZookeeperClient}
import org.slf4j.LoggerFactory

object ZkClient {

  val LOG = LoggerFactory.getLogger(getClass)

  private val zkClientMap=new ConcurrentHashMap[String, ZookeeperClient]()

  private val zkSerializer:ZkSerializer = new ZkSerializer {
    override def serialize(data: scala.Any): Array[Byte] = {
      data.asInstanceOf[String].getBytes("UTF-8")
    }

    override def deserialize(bytes: Array[Byte]): AnyRef = {
      new String(bytes,"UTF-8")
    }
  }

  def get():ZookeeperClient={
    get("")
  }

  def get(path:String):ZookeeperClient={
    var zkClient=zkClientMap.get(path)
    this.synchronized {
      if(zkClient==null){
        zkClient = new ZookeeperClient(
          EnvProperty.get(EnvConstants.ZK_QUORUM)+path,
          EnvProperty.get(EnvConstants.ZK_CONNECTION_TIMEOUT).toInt,
          EnvProperty.get(EnvConstants.ZK_SESSION_TIMEOUT).toInt,
          zkSerializer)
        zkClientMap.put(path,zkClient)
      }
    }
    zkClient
  }

  def createPersistent(zkConstants: ZkConstants.Key)={
    try{
      get().createPersistent(zkConstants.toString)
    }catch{
      case ex:Exception=>
    }
  }

  def createPersistent(path:String)={
    try{
      get().createPersistent(path)
    }catch{
      case ex:Exception=>
    }
  }

  def createPersistent(path:String, data: AnyRef)={
    try{
      get().createPersistent(path, data)
    }catch{
      case ex:Exception=>
    }
  }

}