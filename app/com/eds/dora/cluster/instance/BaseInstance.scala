package com.eds.dora.cluster.instance

import org.apache.commons.logging.LogFactory

trait BaseInstance {

  protected val LOG = LogFactory.getLog(this.getClass)

  def start()

  def stop()

}
