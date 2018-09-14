package com.eds.dora.web.handler

import com.eds.dora.util.ExceptionUtils
import com.eds.dora.web.views.html
import org.slf4j.LoggerFactory
import play.api.http.HttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent._

class ErrorHandler extends HttpErrorHandler {

  val LOG = LoggerFactory.getLogger(getClass)

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    Future.successful(
      BadRequest(html.error(message, "N/A"))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    Future.successful(
      {
        LOG.error("Error ocurred",exception)
        BadRequest(ExceptionUtils.toString(exception))
      }

    )
  }
}