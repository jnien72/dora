package com.eds.dora.cluster

import com.eds.dora.cluster.model.{AgentInfo, InstanceStatus, Topology}
import com.eds.dora.cluster.util.InstanceUtils
import com.eds.dora.util._
import org.I0Itec.zkclient.IZkDataListener
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.sys.process._

object DoraAgent {

  private var agentInfo:AgentInfo=null

  def main(args: Array[String]): Unit = {
    start()
    Thread.sleep(Long.MaxValue)
  }

  def start(): Unit = {
    SysEnv.initZkDirs()

    //if current topology is empty, load the default
    val registeredNameSpaces=ZkClient.get().getChildren(ZkConstants.ZK_NAMESPACE_HOME.toString).size()
    if(registeredNameSpaces==0){
      val topologyJsonStr=Source.fromURL(getClass.getResource("/dora-topology.json")).mkString
      val topology = JsonUtils.fromJson[Topology](topologyJsonStr)
      ClusterAdmin.updateTopology(topology)
    }

    //calculating capacity
    val heapCapacityExp=EnvProperty.get(EnvConstants.AGENT_HEAP_CAPACITY)
    val heapCapacityInMB=InstanceUtils.getHeapSizeInMbFromExpression(heapCapacityExp)
    val usedHeapInMB=0
    val freeHeapInMB=heapCapacityInMB
    val startTime=System.currentTimeMillis()
    agentInfo=AgentInfo(NetworkUtils.getHostname(), startTime,
      heapCapacityInMB,usedHeapInMB,freeHeapInMB)

    val agentZkPath=ZkConstants.ZK_AGENT_HOME.toString + "/" + NetworkUtils.getHostname()
    ZkClient.get().delete(agentZkPath.toString)
    ZkClient.get().createEphemeral(agentZkPath.toString, JsonUtils.toJson(agentInfo))

    //Listen to instance changes
    ZkClient.get().subscribeDataChanges(ZkConstants.ZK_INSTANCE_HOME.toString, instancesChangeListener)

    LoggerFactory.getLogger(getClass).info("Started agent [" + NetworkUtils.getHostname() + "], listening on "+ZkConstants.ZK_INSTANCE_HOME)
    // Check instances status and create instances at startup
    new Thread(instancesUpdateRunnable).run()
  }

  val instancesUpdateRunnable = new Runnable {
    override def run(): Unit = {
      this.synchronized {
        val runningInstances=getRunningInstancesPid()
        val expectedInstances=InstanceUtils.retrieveAllInstanceDetails().map(entry=>{
          val (instanceId,(instanceInfo,instanceStatus))=entry
          if(instanceStatus!=null){
            val isLocal=NetworkUtils.getHostname().equals(instanceStatus.hostname)
            if(isLocal) {
              val isInstanceAlive = runningInstances.find(x => x._2.equals(instanceId)).getOrElse(null) != null
              if (!isInstanceAlive) {
                ZkClient.get().deleteRecursive(ZkConstants.ZK_INSTANCE_HOME + "/" + instanceInfo.getInstanceName() + "/status")
                (instanceId, (instanceInfo, null))
              } else {
                (instanceId, (instanceInfo, instanceStatus))
              }
            }else{
              (instanceId, (instanceInfo, instanceStatus))
            }
          }else{
            (instanceId,(instanceInfo,instanceStatus))
          }
        })

        val expectedRunningInstances=expectedInstances.filter(entry=>{
          val (instanceInfo,instanceStatus)=entry._2
          val isRunning=instanceStatus!=null
          val result=(instanceInfo.enabled && isRunning
            && instanceStatus.hostname.equals(agentInfo.hostname))
          result
        })

        var usedHeapInMB=0

        //check if running instances matches expected instances
        runningInstances.foreach(entry=>{
          val (runningInstancePid,runningInstanceId)=entry
          val expectedRunningInstance=expectedRunningInstances.getOrElse(runningInstanceId,null)
          val isUnexpectedRunningInstance=(expectedRunningInstance==null)
          if(isUnexpectedRunningInstance){
            Runtime.getRuntime().exec(Array[String]("kill","-9",runningInstancePid+""))
            LoggerFactory.getLogger(getClass).info("Stopped instance => ["+runningInstanceId+"]")
          }else{
            val runningInstanceInfo=expectedRunningInstance._1
            usedHeapInMB=usedHeapInMB+runningInstanceInfo.getInstanceHeapSizeInMB()
          }
        })

        //check whether or not start host instances
        expectedInstances.map(entry=>{
          val (instanceInfo,instanceStatus)=entry._2
          val isRunning=(instanceStatus!=null)
          val shouldRun=(!isRunning && instanceInfo.enabled)
          LoggerFactory.getLogger(getClass).info("Checking instance "+instanceInfo.getInstanceId()+" => [isRunning="+isRunning+"] [shouldRun="+shouldRun+"] ")

          if(shouldRun){
            val requiredHeap=instanceInfo.getInstanceHeapSizeInMB()
            val remainingHeap=agentInfo.heapCapacityInMB-usedHeapInMB

            LoggerFactory.getLogger(getClass).info("Current Memory Usage = "+usedHeapInMB+"/"+agentInfo.heapCapacityInMB)
            LoggerFactory.getLogger(getClass).info("Instance "+instanceInfo.getInstanceId()+" requires "+requiredHeap)
            if(requiredHeap<=remainingHeap){
              LoggerFactory.getLogger(getClass).info("Try allocating "+instanceInfo.getInstanceId())
              val startTime=System.currentTimeMillis()
              val status=InstanceStatus(startTime,agentInfo.hostname,0,0)
              val zkPath=ZkConstants.ZK_INSTANCE_HOME+"/"+instanceInfo.getInstanceName()+"/status"
              var allocated:Boolean=false
              try{
                ZkClient.get().createEphemeral(zkPath,JsonUtils.toJson(status))
                allocated=true
              }catch{
                case _:Throwable=>
              }
              if(allocated){
                LoggerFactory.getLogger(getClass).info("Allocated "+instanceInfo.getInstanceId())
                val portRangeStart=EnvProperty.get(EnvConstants.AGENT_INSTANCE_SERVICE_PORT_RANGE_START).toInt
                val portRangeStop=EnvProperty.get(EnvConstants.AGENT_INSTANCE_SERVICE_PORT_RANGE_STOP).toInt
                val instancePort=NetworkUtils.reservePort(portRangeStart,portRangeStop,60)
                val sparkUiPortRangeStart=EnvProperty.get(EnvConstants.AGENT_INSTANCE_SPARK_UI_PORT_RANGE_START).toInt
                val sparkUiPortRangeSop=EnvProperty.get(EnvConstants.AGENT_INSTANCE_SPARK_UI_PORT_RANGE_STOP).toInt
                val sparkUiPort=NetworkUtils.reservePort(sparkUiPortRangeStart,sparkUiPortRangeSop,120)
                status.servicePort=instancePort
                status.sparkUiPort=sparkUiPort
                usedHeapInMB=usedHeapInMB+instanceInfo.getInstanceHeapSizeInMB()
                ZkClient.get().writeData(zkPath,JsonUtils.toJson(status))
                InstanceLauncher.execute(instanceInfo.getInstanceName(),status.servicePort,status.sparkUiPort)


                LoggerFactory.getLogger(getClass).info("Started instance => ["+instanceInfo.getInstanceId()+
                  "] [service.port="+agentInfo.hostname+":"+status.servicePort
                  +", spark.ui.port="+status.sparkUiPort+"]")
              }else{
                LoggerFactory.getLogger(getClass).info("Couldn't allocate "+instanceInfo.getInstanceId()+", seems other agent got it first")
              }
            }else{
              LoggerFactory.getLogger(getClass).info("No enough resource to host "+instanceInfo.getInstanceId())
            }
          }
        })

        agentInfo.usedHeapInMB=usedHeapInMB
        agentInfo.freeHeapInMB=agentInfo.heapCapacityInMB-agentInfo.usedHeapInMB

        //update agent status
        val agentZkPath=ZkConstants.ZK_AGENT_HOME.toString + "/" + NetworkUtils.getHostname()
        ZkClient.get().writeData(agentZkPath.toString, JsonUtils.toJson(agentInfo))
        LoggerFactory.getLogger(getClass).info("Agent instance(s) synchronized")
      }
    }
  }

  private val instancesChangeListener = new IZkDataListener {
    override def handleDataChange(dataPath: String, data: scala.Any): Unit = {
      new Thread(instancesUpdateRunnable).start()
    }
    override def handleDataDeleted(dataPath: String): Unit={}
  }

  def getRunningInstancesPid():Map[Int,String]={
    val clazzName=InstanceLauncher.getClass.getSimpleName.replace("$","")
    val pattern="-Dinstance.id="
    val processes="jps -lmv " !!;
    processes.split("\n").filter(x=> x.contains(clazzName)).map(
      line=>{
        val tokens=line.split(" ")
        val pid=tokens(0).toInt
        var instanceId:String=null
        tokens.foreach(token=>{
          if(token.startsWith(pattern)){
            instanceId=token.substring(pattern.length)
          }
        })
        if(instanceId==null){
          throw new RuntimeException("Unable to retrieve instance id from "+line)
        }
        (pid,instanceId)
      }
    ).toMap
  }
}
