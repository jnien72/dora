package com.eds.dora.util

import java.net.{InetAddress, InetSocketAddress, Socket}

import scala.collection.mutable

object NetworkUtils {

  private var hostname:String=null

  def getHostname():String={
    if(hostname==null){
      hostname=java.net.InetAddress.getLocalHost().getCanonicalHostName();
    }
    hostname
  }

  def isLocalPortInUse(port:Int):Boolean={
    var s:Socket = null
    try {
      s=new Socket()
      val sa = new InetSocketAddress("127.0.0.1", port);
      s.connect(sa, 1000);
      true
    } catch{
      case _=>false
    }finally{
      try{
        s.close()
      }catch{
        case _=>
      }
      false
    }
  }

  def areSameHost(host1:String, host2:String):Boolean={
    val ip1=InetAddress.getByName(host1).getHostAddress
    val ip2=InetAddress.getByName(host2).getHostAddress
    (ip1.equals(ip2))
  }

  def isPortInUse(port:Int):Boolean={
    isPortInUse(getHostname(),port)
  }

  def isPortInUse(host: String, port: Int): Boolean = {
    var s:Socket = null
    try {
      s=new Socket()
      val sa = new InetSocketAddress(host, port)
      s.connect(sa, 1000)
      true
    } catch{
      case e: Throwable => false
    }finally{
      try{
        s.close()
      }catch{
        case e: Throwable =>
      }
    }
  }

  private val reservedPort=mutable.Set[Int]()
  def reservePort(start:Int,stop:Int, timeoutInSeconds:Int):Int={
    var result=0;
    this.synchronized{
      for(port<-start to stop if result==0){
        if(!reservedPort.contains(port)){
          if(!isPortInUse(port)){
            result=port
            reservedPort.add(port)
            new Thread(new Runnable(){
              def run(): Unit ={
                val waitTime=timeoutInSeconds*1000
                Thread.sleep(waitTime)
                reservedPort.remove(port)
              }
            }).start()
          }
        }
      }
    }
    if(result==0){
      throw new RuntimeException("Unable to allocate any port between "+start+" - "+stop)
    }
    result
  }
}