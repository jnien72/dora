package com.eds.dora.web.filters

import play.api.mvc.{Filter, RequestHeader, Result, _}

import scala.concurrent.Future

class AuthFilter extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    var requiresLogin = false
    val path = requestHeader.path
    if (path.endsWith(".html")) {
      if (!path.startsWith("/directLogin.html") && !path.startsWith("/login.html") && !path.startsWith("/logout.html")) {
        requiresLogin = (!requestHeader.session.get("username").isDefined)
      }
    }
    if (requiresLogin) {
      Future.successful(Results.TemporaryRedirect("/login.html"))
    } else {
      nextFilter(requestHeader)
    }
  }
}