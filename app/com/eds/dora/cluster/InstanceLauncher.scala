package com.eds.dora.cluster

import java.io.{InputStream, PrintStream}
import java.util.Scanner

import com.eds.dora.cluster.instance.BaseInstance
import com.eds.dora.cluster.model.InstanceInfo
import com.eds.dora.cluster.util.InstanceUtils
import com.eds.dora.util.{JsonUtils, ZkClient, ZkConstants}
import org.apache.commons.logging.LogFactory

object InstanceLauncher {

  var instance:BaseInstance=null

  def execute(instanceName:String, instancePort:Int, sparkUiPort:Int):Unit={
    val instanceJson=ZkClient.get().readData[String](ZkConstants.ZK_INSTANCE_HOME+"/"+instanceName)
    val instanceInfo=JsonUtils.fromJson[InstanceInfo](instanceJson)
    val instanceId=instanceInfo.getInstanceId()
    val heapSizeExp=instanceInfo.heapSize
    val heapSizeInMB=InstanceUtils.getHeapSizeInMbFromExpression(heapSizeExp)
    val clazz=InstanceLauncher.getClass.getName.replace("$","")
    val cp = System.getProperty("java.class.path")
    val xss=Math.min(64,heapSizeInMB/8)
    val process = Runtime.getRuntime.exec(Array[String](
      "java",
      "-cp",cp,
      "-Dfile.encoding=UTF-8",
      "-XX:+UseG1GC",
      "-XX:MaxGCPauseMillis=20",
      "-XX:InitiatingHeapOccupancyPercent=35",
      "-XX:+UseCompressedOops",
      "-Dhdp.version=2.4.2.0",
      "-Xmx"+heapSizeInMB+"m",
      "-Xms"+heapSizeInMB+"m",
      "-Xss"+xss+"m",
      "-Dinstance.id="+instanceId,
      "-Dservice.port="+instancePort,
      "-Dspark.ui.port="+sparkUiPort,
      clazz
    ))
    inheritIO(process.getInputStream, System.out)
    inheritIO(process.getErrorStream, System.err)
  }

  private def inheritIO(src: InputStream, dest: PrintStream) = {
    new Thread(new Runnable() {
      override def run(): Unit = {
        val sc = new Scanner(src)
        while(sc.hasNextLine) {
          dest.println(sc.nextLine())
        }
      }
    }).start()
  }

  def main(args: Array[String]): Unit = {
    try {
      val instanceId = System.getProperty("instance.id")
      val tokens = instanceId.split("-")
      val namespace = tokens(0)
      val instanceType = tokens(1)
      val instanceName = namespace + "-" + instanceType
      LogFactory.getLog(getClass).info("Starting instance [" + instanceId + "]")

      val prefix = instanceType.charAt(0).toUpper + instanceType.substring(1)
      val clazzName = classOf[BaseInstance].getPackage.getName.replace("/", ".") + "." + prefix + "Instance"

      val clazz = Class.forName(clazzName)
      instance = clazz.newInstance().asInstanceOf[BaseInstance]
      instance.start()
      LogFactory.getLog(getClass).info("Started instance [" + instanceId + "]")
    } catch {
      case t: Throwable => LogFactory.getLog(getClass).error("Error ocurred", t)
    }
  }
}
