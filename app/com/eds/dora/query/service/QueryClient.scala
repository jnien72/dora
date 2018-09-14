package com.eds.dora.query.service

import java.rmi.registry.LocateRegistry

import com.eds.dora.cluster.util.InstanceUtils
import org.slf4j.LoggerFactory


object QueryClient {

  val LOG = LoggerFactory.getLogger(getClass)

  def get(namespace:String):QueryService={
    val name=namespace+"-query"
    val instanceStatus=InstanceUtils.getInstanceStatus(name)
    val connString="//"+instanceStatus.hostname+":"+instanceStatus.servicePort+ "/"+classOf[QueryService].getName
    LOG.info("Instance "+name+" resolved to "+connString)
    val registry = LocateRegistry.getRegistry(instanceStatus.hostname,instanceStatus.servicePort)
    registry.lookup(connString).asInstanceOf[QueryService]
  }
}