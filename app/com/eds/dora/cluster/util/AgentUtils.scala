package com.eds.dora.cluster.util

import com.eds.dora.cluster.model.AgentInfo
import com.eds.dora.util.{JsonUtils, ZkClient, ZkConstants}

object AgentUtils {
  def retrieveAllAgentStatus():List[AgentInfo]= {
    ZkClient.get().getChildren(ZkConstants.ZK_AGENT_HOME.toString).toArray.map(x => {
      val hostname = x.toString
      val agentZkPath = ZkConstants.ZK_AGENT_HOME.toString + "/" + hostname
      val agentData = ZkClient.get().readData[String](agentZkPath)
      JsonUtils.fromJson[AgentInfo](agentData)
    }).toList
  }
}
