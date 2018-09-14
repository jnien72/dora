package com.eds.dora.util

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object JsonUtils {

  val LOG = LoggerFactory.getLogger(getClass())

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def toJson(value: Map[Symbol, Any]): String = {
    toJson(value map { case (k, v) => k.name -> v })
  }

  def toJson(value: Any): String = {
    toJson(value, false)
  }

  def toJson(value: Any, prettyPrint: Boolean): String = {
    if (!prettyPrint)
      mapper.writeValueAsString(value)
    else
      mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
  }

  def toMap[V](json: String)(implicit m: Manifest[V]) = fromJson[Map[String, V]](json)

  def fromJson[T](json: String)(implicit m: Manifest[T]): T = {
    mapper.readValue[T](json)
  }

  def fromBytes(bytes: Array[Byte], clazz: Class[_]): AnyRef = {
    mapper.readValue(bytes, clazz).asInstanceOf[AnyRef]
  }

  def toBytes(value: Any): Array[Byte] = {
    mapper.writeValueAsBytes(value)
  }

  def convertJsonToArray(json: String, columns: Array[String]): Array[String] = {
    val jsonParser = new JsonParser()
    val jsonElement = jsonParser.parse(json)
    val map = jsonElement.getAsJsonObject().entrySet().asScala
      .foldLeft(Map[String, String]())((map, element) => {
        map + (element.getKey -> element.getValue().toString.replaceAll("\t", " "))
      })
    columns.map(field => {
      if(map.contains(field)) {
        map.get(field).get
      } else {
        "NULL"
      }
    })
  }
}