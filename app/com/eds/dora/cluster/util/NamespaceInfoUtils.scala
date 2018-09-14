package com.eds.dora.cluster.util

import com.eds.dora.cluster.model.NamespaceInfo
import com.eds.dora.util.{JsonUtils, ZkClient, ZkConstants}

import scala.collection.JavaConverters._
object NamespaceInfoUtils {

  def listAll(): List[NamespaceInfo] = {
    try{
      ZkClient.get().getChildren(ZkConstants.ZK_NAMESPACE_HOME.toString).asScala
        .map(ns => {
          val namespace = get(ns)
          if(namespace.isDefined) {
            namespace.get
          } else {
            null
          }
        })
        .filter(ns => ns != null)
        .toList
    }catch{
      case t:Throwable=>List()
    }
  }

  def get(namespace: String): Option[NamespaceInfo] = {
    try{
      Option(JsonUtils.fromJson[NamespaceInfo](ZkClient.get().readData[String](ZkConstants.ZK_NAMESPACE_HOME.toString + "/" + namespace)))
    }catch{
      case t:Throwable=>None
    }
  }
}
