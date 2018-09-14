package com.eds.dora.web.filters

import com.eds.dora.util.{EnvConstants, EnvProperty}
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.Future
import scala.util.Random


//this filter just simulates a slow connection
class WebDelayFilter extends Filter {

  val min=EnvProperty.get(EnvConstants.WEB_DELAY_MILLIS_MIN).toInt
  val delay=EnvProperty.get(EnvConstants.WEB_DELAY_MILLIS_MAX).toInt-min
  val enabled=(delay>0)
  val rand=new Random()

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    if(enabled){
      if(requestHeader.path.contains(".json") || requestHeader.path.contains(".html")){
        Thread.sleep(min+rand.nextInt(delay))
      }
    }
    nextFilter(requestHeader)
  }
}