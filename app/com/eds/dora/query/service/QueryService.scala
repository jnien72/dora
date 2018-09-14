package com.eds.dora.query.service

import java.rmi.{Remote, RemoteException}

import com.eds.dora.query.model.{QueryResponse, QueryStatus}
import com.healthmarketscience.rmiio.SerializableInputStream

trait QueryService extends Remote{

  @throws(classOf[RemoteException])
  def query(user: String, jobId: String, sql: String, numOfRows: Int, txDate: String): QueryResponse

  @throws(classOf[RemoteException])
  def queryAsStream(user: String, jobId: String, sql: String, numOfRows: Int, txDate: String): (Option[Array[String]], SerializableInputStream)

  @throws(classOf[RemoteException])
  def stop(user:String, jobId: String): Unit

  @throws(classOf[RemoteException])
  def meta(user: String, scope: String, target: String): Array[String]

  @throws(classOf[RemoteException])
  def status(user: String):List[QueryStatus]

  @throws(classOf[RemoteException])
  def listTemplates(user:String):Map[String,String]

  @throws(classOf[RemoteException])
  def setTemplate(user:String, name:String, sql:String):Unit

  @throws(classOf[RemoteException])
  def deleteTemplate(user:String, name:String):Unit
}