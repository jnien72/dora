package com.eds.dora.util

import com.typesafe.config.ConfigFactory

import scala.io.Source


object EnvProperty {

  def getKeysWithPrefix(prefix: String): Map[String, String] = {
    Source.fromURL(getClass.getResource("/application.conf")).getLines()
      .map(x => x.trim).filter(x => (!x.startsWith("#"))).map(
      x => (if (x.indexOf("=") == x.lastIndexOf("=")) {
        val key = x.split("=")(0)
        try {
          (key, resolveConfig(key))
        } catch {
          case x: Throwable => (key, null)
        }
      } else (x, null))
    ).toMap.filter(x => (x._2 != null && x._1.startsWith(prefix)))
  }

  private val config = ConfigFactory.load()

  private def resolveConfig(key: String): String = {
    var result = config.getString(key)
    while (result.contains("${")) {
      val subsKey = result.substring(result.indexOf("${") + 2, result.indexOf("}"))
      result = result.replace("${" + subsKey + "}", config.getString(subsKey))
    }
    result
  }

  def get(key: EnvConstants.Key): String = {
    resolveConfig(key.toString)
  }
}
