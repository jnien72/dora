package com.eds.dora.cluster.util

import com.eds.dora.cluster.model.UserInfo
import com.eds.dora.util.{JsonUtils, ZkClient, ZkConstants}

object UserInfoUtils {

  def get(username: String): Option[UserInfo] = {
    try{
      JsonUtils.fromJson[List[UserInfo]](ZkClient.get().readData[String](ZkConstants.ZK_USER_HOME.toString))
        .find(_.username.equals(username))
    }catch{
      case t:Throwable=>None
    }
  }
}
