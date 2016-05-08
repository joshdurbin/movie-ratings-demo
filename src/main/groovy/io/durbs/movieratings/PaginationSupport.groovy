package io.durbs.movieratings

import groovy.transform.CompileStatic
import io.durbs.movieratings.config.APIConfig
import ratpack.handling.Context

@CompileStatic
class PaginationSupport {

  final Context context
  final APIConfig apiConfig

  PaginationSupport(Context context, APIConfig apiConfig) {

    this.context = context
    this.apiConfig = apiConfig
  }

  Integer getPageNumber() {

    final Integer suppliedPageNumber

    if ((context.request.queryParams.get(Constants.PAGE_NUMBER_QUERY_PARAM_KEY) as CharSequence)?.isNumber()) {
      suppliedPageNumber = (context.request.queryParams.get(Constants.PAGE_NUMBER_QUERY_PARAM_KEY) as Integer).abs()
    } else {
      suppliedPageNumber = apiConfig.defaultFirstPage
    }

    suppliedPageNumber
  }

  Integer getPageSize() {

    final Integer suppliedPageSize

    if ((context.request.queryParams.get(Constants.PAGE_SIZE_QUERY_PARAM_KEY) as CharSequence)?.isNumber()) {
      suppliedPageSize = (context.request.queryParams.get(Constants.PAGE_SIZE_QUERY_PARAM_KEY) as Integer).abs()
    } else {
      suppliedPageSize = apiConfig.defaultResultsPageSize
    }

    final Integer limit

    if (suppliedPageSize > apiConfig.maxResultsPageSize || suppliedPageSize == 0) {
      limit = apiConfig.maxResultsPageSize
    } else {
      limit = suppliedPageSize
    }

    limit
  }

  Integer getOffSet() {
    pageNumber * pageSize
  }
}
