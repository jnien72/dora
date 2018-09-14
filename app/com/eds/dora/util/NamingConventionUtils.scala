package com.eds.dora.util

import scala.collection.immutable.HashSet

object NamingConventionUtils {

  private val validChars="0123456789abcdefghijklmnopqrstuvwxyz_"

  def validateConnectionName(name:String): Unit={
    if(name ==null){
      throw new RuntimeException("Unspecified connection name")
    }
    if(name.length<1 || name.length>128){
      throw new RuntimeException("Connection name length must be between 1 and 128 characters")
    }
    name.foreach(x=>{
      if(validChars.indexOf(x)<0){
        throw new RuntimeException("Connection name '"+name+"' contains invalid chars, valid chars are [a-z] or [0-9] or '_'")
      }
    })
    if(name.charAt(0)<'a'&&name.charAt(0)>'z'){
      throw new RuntimeException("Connection name '"+name+"' must start with [a-z]")
    }
  }

  def validateTableName(name:String): Unit={
    if(name ==null){
      throw new RuntimeException("Unspecified table name")
    }
    if(name.length<1 || name.length>128){
      throw new RuntimeException("Table name length must be between 1 and 128 characters")
    }
    name.foreach(x=>{
      if(validChars.indexOf(x)<0){
        throw new RuntimeException("Table name '"+name+"' contains invalid chars, valid chars are [a-z] or [0-9] or '_'")
      }
    })
    if(name.charAt(0)<'a'&&name.charAt(0)>'z'){
      throw new RuntimeException("Table name '"+name+"' must start with [a-z]")
    }
  }

  def validateFieldName(name:String): Unit ={
    if(name ==null){
      throw new RuntimeException("Unspecified field name")
    }
    if(name.length<1 || name.length>128){
      throw new RuntimeException("Field name length must be between 1 and 128 characters")
    }
    name.foreach(x=>{
      if(validChars.indexOf(x)<0){
        throw new RuntimeException("Field name '"+name+"' contains invalid chars, valid chars are [a-z] or [0-9] or '_'")
      }
    })
  }

  val validFieldTypes=HashSet[String]("string","int","double","float","long")
  def validateFieldType(name:String):Unit={
    if(!validFieldTypes.contains(name)){
      throw new RuntimeException("Field type '"+name+"' is not supported, supported types are "+JsonUtils.toJson(validFieldTypes))
    }
  }
}
