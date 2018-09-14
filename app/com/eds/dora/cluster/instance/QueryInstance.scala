package com.eds.dora.cluster.instance

import com.eds.dora.query.service.QueryServiceImpl
import com.eds.dora.util.SysEnv

class QueryInstance extends BaseInstance {

  @Override
  override def start(): Unit = {
    SysEnv.loadHadoopConf()
    QueryServiceImpl.init()
  }

  @Override
  override def stop(): Unit = {
    throw new RuntimeException("Unsupported operation")
  }
}