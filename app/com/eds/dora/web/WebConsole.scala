package com.eds.dora.web

import com.eds.dora.util.{EnvConstants, EnvProperty, ZkClient}
import org.apache.ivy.util.HostUtil
import org.slf4j.LoggerFactory
import play.api._
import play.core.server.{RealServerProcess, ServerConfig, ServerProvider, ServerWithStop}

object WebConsole {

  private val LOG = LoggerFactory.getLogger(getClass)

  private var server: ServerWithStop = null

  def main(args: Array[String]): Unit = {
    try{
      System.setProperty("log.name", "agent")
      val process = new RealServerProcess(args)
      val config: ServerConfig = play.core.server.ProdServerStart.readServerConfigSettings(process)
      val application: Application = {
        val environment = Environment(config.rootDir, process.classLoader, Mode.Prod)
        val context = ApplicationLoader.createContext(environment)
        val loader = ApplicationLoader(context)
        loader.load(context)
      }
      Play.start(application)
      val serverProvider: ServerProvider = ServerProvider.fromConfiguration(process.classLoader, config.configuration)
      server = serverProvider.createServer(config, application)
    }catch {
      case t: Throwable => LOG.error("Error occurred", t)
    }
  }
}