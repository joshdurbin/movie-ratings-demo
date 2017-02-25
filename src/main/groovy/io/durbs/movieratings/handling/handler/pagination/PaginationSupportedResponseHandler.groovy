package io.durbs.movieratings.handling.handler.pagination

import com.google.inject.Singleton
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.util.logging.Slf4j
import io.durbs.movieratings.Constants
import io.durbs.movieratings.MovieRatingsConfig
import io.durbs.movieratings.model.Movie
import org.apache.commons.lang.math.NumberUtils
import ratpack.groovy.handling.GroovyHandler

@Singleton
@Slf4j
@CompileStatic
abstract class PaginationSupportedResponseHandler extends GroovyHandler {

  abstract MovieRatingsConfig getMovieRatingsConfig()

  abstract Map<String, String> getRequestParameters()

  Integer getPageNumber() {

    Integer suppliedPageNumber

    if (requestParameters.get(Constants.PAGE_NUMBER_QUERY_PARAM_KEY)?.isNumber()) {
      suppliedPageNumber = (requestParameters.get(Constants.PAGE_NUMBER_QUERY_PARAM_KEY) as Integer).abs()
    } else {
      suppliedPageNumber = movieRatingsConfig.defaultFirstPage
    }

    suppliedPageNumber
  }

  Integer getPageSize() {

    Integer suppliedPageSize

    if (requestParameters.get(Constants.PAGE_SIZE_QUERY_PARAM_KEY)?.isNumber()) {
      suppliedPageSize = (requestParameters.get(Constants.PAGE_SIZE_QUERY_PARAM_KEY) as Integer).abs()
    } else {
      suppliedPageSize = movieRatingsConfig.defaultResultsPageSize
    }

    Integer limit

    if (suppliedPageSize > movieRatingsConfig.maxResultsPageSize || suppliedPageSize == NumberUtils.INTEGER_ZERO) {
      limit = movieRatingsConfig.maxResultsPageSize
    } else {
      limit = suppliedPageSize
    }

    limit
  }

  static class MovieListWithCursor {

    List<Movie> movies
    String cursorURI

    MovieListWithCursor(List<Movie> movies,
                        Integer pageNumber,
                        Integer pageSize,
                        Integer totalMovieCount) {

      this.movies = movies
      cursorURI = ''
    }
  }
}
