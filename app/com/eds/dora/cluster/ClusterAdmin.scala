package com.eds.dora.cluster

import java.io._
import java.lang.reflect.Parameter

import com.eds.dora.annotation.{ExportedFunction, ExportedParam}
import com.eds.dora.cluster.model.{InstanceInfo, Topology}
import com.eds.dora.cluster.util.{AgentUtils, InstanceUtils}
import com.eds.dora.dao.MetaTable
import com.eds.dora.util._
import com.google.common.base.Strings
import org.apache.commons.logging.LogFactory

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.control.Breaks._

object ClusterAdmin {

  @ExportedFunction(name = "topology", description = "Load topology configuration and refresh instances")
  def topology(@ExportedParam(name="topologyConfig") topologyDescriptor:String): Unit = {
    LogFactory.getLog(getClass).info("Loading topology from "+topologyDescriptor)

    val file = new File(topologyDescriptor)
    if(!file.exists()){
      throw new RuntimeException("File "+topologyDescriptor+" doesn't exist")
    }
    val strConfiguration=Source.fromFile(file).getLines().mkString("\n")
    val topology = JsonUtils.fromJson[Topology](strConfiguration)
    updateTopology(topology)
  }

  def updateTopology(topology: Topology): Unit = {
    val configuredInstances=mutable.HashMap[String,InstanceInfo]()
    topology.namespaceList.foreach(ns=>{
      ns.instances.foreach(instance=>{
        instance.namespace=ns.name
        if(!ns.enabled){
          instance.enabled=false
        }
        configuredInstances.put(instance.namespace+"-"+instance.instanceType, instance)
      })
    })

    LogFactory.getLog(getClass).info("Start updating topology instance(s):")

    //remove all instances that are not specified or changed from topology
    val zkInstances=ZkClient.get().getChildren(ZkConstants.ZK_INSTANCE_HOME.toString).toArray.map(x=>x.toString)
    zkInstances.foreach(instanceId=>{
      if(!configuredInstances.contains(instanceId)){
        ZkClient.get().deleteRecursive(ZkConstants.ZK_INSTANCE_HOME.toString+"/"+instanceId)
        LogFactory.getLog(getClass).info("Removing instance => "+instanceId +" (delete)")
      }else{
        val zkInstanceInfoStr=ZkClient.get().readData[String](ZkConstants.ZK_INSTANCE_HOME.toString+"/"+instanceId)
        val zkInstanceInfo=JsonUtils.fromJson[InstanceInfo](zkInstanceInfoStr)
        val configuredInstanceInfo=configuredInstances.get(instanceId).getOrElse(null)
        if(!zkInstanceInfo.getInstanceId().equals(configuredInstanceInfo.getInstanceId())){
          ZkClient.get().deleteRecursive(ZkConstants.ZK_INSTANCE_HOME.toString+"/"+instanceId)
          LogFactory.getLog(getClass).info("Removing instance => "+instanceId +" (temporary, will be re-deployed)")
        }
      }

    })

    configuredInstances.foreach(x=>{
      val (instanceId,instanceInfo)=x
      val instanceInfoJson=JsonUtils.toJson(instanceInfo)
      if(!ZkClient.get().exists(ZkConstants.ZK_INSTANCE_HOME.toString+"/"+instanceId)){
        ZkClient.get().createPersistent(ZkConstants.ZK_INSTANCE_HOME.toString+"/"+instanceId,instanceInfoJson)
        LogFactory.getLog(getClass).info("Adding instance => "+instanceId)
      }
    })

    LogFactory.getLog(getClass).info("Finished updating topology instance(s)")

    ZkClient.get().writeData(ZkConstants.ZK_INSTANCE_HOME.toString,System.currentTimeMillis()+"-"+System.nanoTime())

    //update namespace configuration
    val zkNamespaces=ZkClient.get().getChildren(ZkConstants.ZK_NAMESPACE_HOME.toString).toArray.map(x=>x.toString)

    //delete unused
    val configuredNameSpaces=topology.namespaceList.map(ns=>(ns.name,ns)).toMap
    zkNamespaces.foreach(zkNamespace=>{
      if(configuredNameSpaces.getOrElse(zkNamespace,null)==null){
        ZkClient.get().deleteRecursive(ZkConstants.ZK_NAMESPACE_HOME+"/"+zkNamespace)
      }
    })
    configuredNameSpaces.map(ns=>{
      val (nsName,nsInfo)=ns
      val nsInfoJson=JsonUtils.toJson(nsInfo)
      val zkPath=ZkConstants.ZK_NAMESPACE_HOME+"/"+nsName
      if(!ZkClient.get().exists(zkPath)){
        ZkClient.get().createPersistent(zkPath,nsInfoJson)
      }else{
        ZkClient.get().writeData(zkPath,nsInfoJson)
      }
      ZkClient.createPersistent(zkPath+"/metadata")
      MetaTable.getClass.getDeclaredFields.filter(field => (!field.getName.contains('$')))
        .map(field => {
          field.setAccessible(true)
          val tableName=field.get(MetaTable).toString
          ZkClient.createPersistent(zkPath+"/metadata/"+tableName)
        })
    })

    //update user configuration
    val userConfiguration=JsonUtils.toJson(topology.userList)
    ZkClient.get().writeData(ZkConstants.ZK_USER_HOME.toString,userConfiguration)

    //update topology on zk
    ZkClient.get().writeData(ZkConstants.ZK_TOPOLOGY_HOME.toString, JsonUtils.toJson(topology, true))
  }

  @ExportedFunction(name = "restart", description = "Display cluster status")
  def restartInstance(@ExportedParam(name="instance") instance:String): Unit = {
    val tokens=instance.split("-")
    if(tokens.length!=2){
      throw new RuntimeException("Instance format should be [namespace]-[instanceType]")
    }
    val zkPath=ZkConstants.ZK_INSTANCE_HOME.toString+"/"+instance
    if(!ZkClient.get().exists(zkPath)){
      throw new RuntimeException("Instance '" + instance + "' not found")
    }
    LogFactory.getLog(getClass).info("Un-deploying "+instance+"...")
    val zkData=ZkClient.get().readData[String](zkPath).toString
    ZkClient.get().deleteRecursive(zkPath)
    ZkClient.get().writeData(ZkConstants.ZK_INSTANCE_HOME.toString,System.currentTimeMillis()+"-"+System.nanoTime())
    Thread.sleep(1000)
    LogFactory.getLog(getClass).info("Re-deploying "+instance+"...")
    ZkClient.createPersistent(zkPath,zkData)
    ZkClient.get().writeData(ZkConstants.ZK_INSTANCE_HOME.toString,System.currentTimeMillis()+"-"+System.nanoTime())
  }

  @ExportedFunction(name = "status", description = "Display cluster status")
  def status(): Unit = {

    val instanceStatus=InstanceUtils.retrieveAllInstanceDetails()

    println("Instances:")
    println()

    instanceStatus.foreach(x=>{
      val (instanceId,(instanceInfo,instanceStatus))=x
      val heapSize=instanceInfo.heapSize
      val enabled=instanceInfo.enabled
      var hostname="n/a"
      var status="n/a"
      var port="n/a"
      if(instanceStatus!=null){
        status="running"
        hostname=instanceStatus.hostname
        if(instanceStatus.servicePort>0){
          port=instanceStatus.servicePort+""
        }
      }
      println(" [" + instanceId + "] => [ enabled="+enabled+", status=" + status + ", hostname=" + hostname +  ", " +
        "heapSize=" + heapSize + ", port=" + port +" ]")
    })


    println()
    println("Agents:")
    AgentUtils.retrieveAllAgentStatus().foreach(agent=>{
      println( " [" + agent.hostname + "] => [ uptime="
        +DateTimeUtils.getTimeDiffExp(agent.startTime,System.currentTimeMillis())
        +", total_heap="+agent.heapCapacityInMB+"MB"
        +", used_heap="+agent.usedHeapInMB+"MB"
        +", free_heap="+agent.freeHeapInMB+"MB"
        +" ]"
      )
    })
  }

  case class Metadata(namespace:String, table:String, tableMaps: List[Map[String, Any]])

  @ExportedFunction(name = "meta-export", description = "Export metadata")
  def exportMeta(@ExportedParam(name="export path") exportPath: String): Unit = {
    val metadataList = ListBuffer[Metadata]()
    var bw: BufferedWriter = null
    try {
      ZkClient.get().getChildren(ZkConstants.ZK_NAMESPACE_HOME.toString).asScala.foreach(namespace => {
        ZkClient.get().getChildren(ZkConstants.ZK_NAMESPACE_HOME.toString+"/"+namespace+"/metadata").asScala.foreach(table => {
          val zkPath = ZkConstants.ZK_NAMESPACE_HOME.toString+"/"+namespace+"/metadata/"+table
          val mapList = ZkClient.get().getChildren(zkPath).asScala.map(key => {
            JsonUtils.fromJson[Map[String, Any]](ZkClient.get().readData[String](zkPath + "/" + key))
          })
          if(mapList.length > 0) {
            println("Metadata with [" + namespace + "][" + table + "] is extracted")
            metadataList += Metadata(namespace, table, mapList.toList)
          }
        })
      })

      bw = new BufferedWriter(new FileWriter(new File(exportPath)))
      if(metadataList.length > 0) {
        println("Metadata is exporting...")
        bw.write(JsonUtils.toJson(metadataList.toList, true) + System.lineSeparator())
        println("Metadata is exported to path [" + exportPath + "]")
      }
    } catch {
      case ignored: Exception => {
        println("Error occurred: " + ignored)
      }
    } finally {
      if(bw != null) {
        bw.close()
      }
    }
  }

  @ExportedFunction(name = "meta-import", description = "Import metadata")
  def importMeta(@ExportedParam(name="import path") importPath: String): Unit = {
    var br: BufferedReader = null
    var line = ""
    val sb = new StringBuilder()
    try {
      br = new BufferedReader(new FileReader(new File(importPath)))
      line = br.readLine()
      while(line != null) {
        sb.append(line)
        line = br.readLine()
      }
      val metadataList = JsonUtils.fromJson[List[Metadata]](sb.toString())
      metadataList.foreach(metadata => {
        val zkPath = ZkConstants.ZK_NAMESPACE_HOME+"/"+metadata.namespace+"/metadata/"+metadata.table
        metadata.tableMaps.foreach(map => {
          val name = if(MetaTable.QUERY_TEMPLATE.toString.equals(metadata.table)) {
            Option(metadata.namespace)
          } else if (MetaTable.CONNECTION.toString.equals(metadata.table)) {
            map.get("connectionName")
          } else {
            map.get("name")
          }
          if(name.isDefined) {
            val dataPath = zkPath + "/" + name.get
            println("Metadata is importing to [" + dataPath + "]")
            ZkClient.createPersistent(dataPath)
            ZkClient.get().writeData(dataPath, JsonUtils.toJson(map))
          } else {
            throw new RuntimeException("Required field with [" + metadata.namespace + "][" + metadata.table + "] is not found: " + map)
          }
        })
      })
      println("All metadata is imported")
    } catch {
      case ignored: Exception => {
        println("Error occurred: " + ignored)
      }
    } finally {
      if(br != null) {
        br.close()
      }
    }
  }

  def main(args: Array[String]) = {

    System.setProperty("log.name", "cluster-admin")

    if (args.length == 0) {
      printUsage()
      System.exit(-1)
    }

    SysEnv.initZkDirs()

    var found: Boolean = false
    val func: String = args(0)
    for (m <- getClass.getMethods) {
      val expFunc: ExportedFunction = m.getAnnotation(classOf[ExportedFunction])
      if (expFunc != null && expFunc.name.equals(func) && m.getParameterCount == args.length - 1) {
        try {
          found = true

          val paramsAsObjects: java.util.List[Object] = new java.util.ArrayList[Object]()
          val size = args.length - 1
          var i = 0
          while (i < size) {
            val parsed: Object = parseParam(args(i + 1), m.getParameters()(i)).asInstanceOf[Object]
            paramsAsObjects.add(parsed)
            i = i + 1
          }
          val result: Any = m.invoke(this, paramsAsObjects.toArray(): _*)
          if (result != null && classOf[Void] != result.getClass && classOf[Unit] != result.getClass) {
            if (result.isInstanceOf[String]) {
              LogFactory.getLog(getClass).info("\n" + result)
            }
            else {
              LogFactory.getLog(getClass).info("\n" + JsonUtils.toJson(result))
            }
          }
        } catch {
          case t: Throwable => {
            t.printStackTrace()
            LogFactory.getLog(getClass).error("Error occurred", t)
          }
        }
      }
    }
    if (!found) {
      printUsage()
      System.exit(-1)
    }
  }

  private def parseParam(paramValue: String, param: Parameter): Any = {
    if (param.getType eq classOf[String]) {
      return paramValue
    }
    else if (param.getType eq classOf[Boolean]) {
      if (paramValue.equalsIgnoreCase("true")) {
        return true
      } else if (paramValue.equalsIgnoreCase("false")) {
        return false
      }
    }
    else if (param.getType eq classOf[Integer]) {
      return paramValue.toInt
    }
    else if (param.getType eq classOf[Long]) {
      return paramValue.toLong
    }
    else if (param.getType eq classOf[Double]) {
      return paramValue.toDouble
    }
    else if (param.getType eq classOf[Float]) {
      return paramValue.toFloat
    }
    throw new RuntimeException("Unable to parse " + paramValue + " to " + param.getType.getSimpleName)
  }

  private def printUsage(): Unit = {
    println("Usage admin {function} {args}")
    println("Available Functions:")
    val funcList = ListBuffer[String]()
    for (m <- getClass.getMethods) {
      var paramDescription: String = " "
      val expFunc: ExportedFunction = m.getAnnotation(classOf[ExportedFunction])
      breakable {
        if (expFunc == null) {
          break
        }
        if (m.getParameterCount > 0) {
          val params: Array[Parameter] = m.getParameters
          for (param <- params) {
            val exportedParam: ExportedParam = param.getAnnotation(classOf[ExportedParam])
            if (exportedParam != null) {
              paramDescription += "[" + exportedParam.name + "] "
            }
          }
          funcList += Strings.padEnd(expFunc.name, 10, ' ') + " " + Strings.padEnd(paramDescription, 35, ' ') + " " + expFunc.description
        } else {
          funcList += Strings.padEnd(expFunc.name, 10, ' ') + expFunc.description
        }
      }
    }
    funcList.sorted.foreach(func => println(func))
  }
}
