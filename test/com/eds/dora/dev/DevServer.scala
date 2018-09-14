package com.eds.dora.dev

import java.io.File
import java.util.Properties

import com.eds.dora.util.{EnvConstants, EnvProperty, NetworkUtils}
import org.apache.commons.io.FileUtils
import org.apache.zookeeper.server.quorum.QuorumPeerConfig
import org.apache.zookeeper.server.{ServerConfig, ZooKeeperServerMain}
import org.slf4j.LoggerFactory

object DevServer {

  val localDirPath=EnvProperty.get(EnvConstants.TMP_DIR)

  var zkThread:Thread=null
  var zkServer:ZooKeeperServerMain=null

  def main(args:Array[String]): Unit ={
    System.setProperty("log.name","dev-server")
    cleanup()
    startLocalZk()
  }

  def cleanup(): Unit ={
    val localDir=new File(localDirPath)
    FileUtils.deleteQuietly(localDir)
    FileUtils.forceMkdir(localDir)
  }

  def startLocalZk(): Unit ={
    if(NetworkUtils.isPortInUse("localhost", 2181)){
      throw new RuntimeException("Port 2181 required by ZooKeeper is already in use")
    }
    val zkProperties:Properties=new Properties()
    zkProperties.setProperty("clientPort","2181")
    zkProperties.setProperty("dataDir",localDirPath+"/zkData")

    val quorumConfiguration:QuorumPeerConfig = new QuorumPeerConfig()
    quorumConfiguration.parseProperties(zkProperties)

    val serverConfig:ServerConfig = new ServerConfig()
    serverConfig.readFrom(quorumConfiguration)

    LoggerFactory.getLogger(getClass).info("starting ZK server on port "+zkProperties.getProperty("clientPort"))
    new Thread(new Runnable(){
      override def run(): Unit = {
        zkServer=new ZooKeeperServerMain()
        zkServer.runFromConfig(serverConfig)

      }
    }).start()
  }

  def stopZooKeeper()={
    val method=zkServer.getClass.getDeclaredMethod("shutdown")
    method.setAccessible(true)
    method.invoke(zkServer)
  }

}