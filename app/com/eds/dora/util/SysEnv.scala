package com.eds.dora.util

import java.io.File
import java.lang.reflect.Method
import java.net.{URL, URLClassLoader}

import org.slf4j.LoggerFactory

object SysEnv {

  val LOG = LoggerFactory.getLogger(getClass)

  def initZkDirs(): Unit = {
    ZkConstants.getClass.getDeclaredFields.filter(field => (!field.getName.contains('$')))
      .map(field => {
        field.setAccessible(true)
        val path=field.get(ZkConstants).toString
        ZkClient.createPersistent(path)
      })
  }

  def loadHadoopConf(): Unit = {
    val hadoopConfDir = new File(EnvProperty.get(EnvConstants.HADOOP_CONF_DIR))
    if (hadoopConfDir.exists()) {
      addFile(hadoopConfDir)
      LOG.info("Loaded hadoop configuration from " + hadoopConfDir.getPath)
    }
  }


  private def addFile(filePath: String) {
    val f: File = new File(filePath);
    addFile(f)
  }

  private def addFile(f: File) {
    addURL(f.toURI().toURL());
  }

  private def addURL(u: URL) {
    val sysLoader: URLClassLoader = ClassLoader.getSystemClassLoader().asInstanceOf[URLClassLoader]
    val sysClass = classOf[URLClassLoader];
    val method: Method = sysClass.getDeclaredMethod("addURL", classOf[URL]);
    method.setAccessible(true)
    method.invoke(sysLoader, u)
  }
}