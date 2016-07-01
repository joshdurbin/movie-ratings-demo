package io.durbs.movieratings.handling.handler.pagination

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

import static com.mongodb.client.model.Filters.text

@Singleton
@Slf4j
@CompileStatic
class MovieSearchHandler extends PaginationSupportedResponseHandler {

  @Inject
  MovieRatingsConfig movieRatingsConfig

  @Inject
  MovieService movieService

  Map<String, String> params

  @Override
  protected void handle(GroovyContext context) {

    params = context.request.queryParams

    final String queryTerm = context.request.queryParams.get('q', Constants.EMPTY_STRING)

    final Observable<List<Movie>> moviesObservable = movieService
      .getAllMovies(text(queryTerm), pageNumber, pageNumber * pageSize)
      .toList()
      .defaultIfEmpty([])

    final Observable<Long> movieCountObservable = movieService
      .getMovieCount(text(queryTerm))
      .defaultIfEmpty(0L)

    Observable.zip(moviesObservable, movieCountObservable, { movies, movieCount ->

      log.info("movie count is ${movieCount}")

      movies
    } as Func2)
      .subscribe { final List<Movie> movies ->

      context.render Jackson.json(movies)
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
