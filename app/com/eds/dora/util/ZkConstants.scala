package com.eds.dora.util

object ZkConstants extends Enumeration {
  type Key = Value

  val ZK_HOME = Value("/dora")
  val ZK_USER_HOME = Value(ZK_HOME + "/user")
  val ZK_TOPOLOGY_HOME = Value(ZK_HOME + "/topology")
  val ZK_AGENT_HOME = Value(ZK_HOME + "/agent")
  val ZK_NAMESPACE_HOME = Value(ZK_HOME + "/namespace")
  val ZK_INSTANCE_HOME = Value(ZK_HOME + "/instance")
}