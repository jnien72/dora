package com.eds.dora.web.filters

import javax.inject.Inject

import play.api.http.HttpFilters

class FilterContainer @Inject()(auth: AuthFilter, webDelayFilter: WebDelayFilter) extends HttpFilters {
  val filters = Seq(auth, webDelayFilter)
}