package io.durbs.movieratings.handling.handler.pagination

import com.google.common.net.HttpHeaders
import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.Constants
import io.durbs.movieratings.MovieRatingsConfig
import io.durbs.movieratings.model.Movie
import io.durbs.movieratings.services.MovieService
import ratpack.groovy.handling.GroovyContext
import ratpack.jackson.Jackson
import rx.Observable
import rx.functions.Func2

@Singleton
@Slf4j
@CompileStatic
class GetAllMoviesHandler extends PaginationSupportedResponseHandler {

  @Inject
  MovieRatingsConfig movieRatingsConfig

  @Inject
  MovieService movieService

  Map<String, String> params

  @Override
  protected void handle(GroovyContext context) {

    params = context.request.queryParams

    Observable<List<Movie>> moviesObservable = movieService
      .getAllMovies(Constants.EMPTY_DOCUMENT, pageNumber, pageSize)
      .toList()
      .defaultIfEmpty([])

    Observable<Long> movieCountObservable = movieService
      .getMovieCount(Constants.EMPTY_DOCUMENT)
      .defaultIfEmpty(0L)

    Observable.zip(moviesObservable, movieCountObservable, { List<Movie> movies, Long movieCount ->

      new MovieListWithCursor(movies, pageNumber, pageSize, movieCount as Integer)
    } as Func2)
      .subscribe { MovieListWithCursor movieListWithCursor ->

      context.response.headers.add(HttpHeaders.LINK, movieListWithCursor.cursorURI)
      context.render Jackson.json(movieListWithCursor.movies)
    }
  }

  @Override
  MovieRatingsConfig getMovieRatingsConfig() {
    movieRatingsConfig
  }

  @Override
  Map<String, String> getRequestParameters() {
    params
  }
}
