package com.eds.dora.util

import scala.concurrent.duration._
import scala.concurrent.{Await, _}

object TimedBlockUtils {

  def timeout[T](timeoutMillis:Long, awaitable:Awaitable[T])(implicit m: Manifest[T]): T = {
    try{
      Await.result(awaitable, timeoutMillis millis)
    }catch{
      case ex:TimeoutException=>throw new TimeoutException("Request timed out after waiting for "+timeoutMillis+" milliseconds")
    }
  }
}
