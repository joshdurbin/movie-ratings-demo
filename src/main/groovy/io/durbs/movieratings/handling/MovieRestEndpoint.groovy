package io.durbs.movieratings.handling

import com.google.common.collect.Range
import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.util.logging.Slf4j
import io.durbs.movieratings.PaginationSupport
import io.durbs.movieratings.config.APIConfig
import io.durbs.movieratings.handling.auth.JWTTokenHandler
import io.durbs.movieratings.model.persistent.Movie
import io.durbs.movieratings.model.persistent.RatedMovie
import io.durbs.movieratings.model.persistent.Rating
import io.durbs.movieratings.model.persistent.User
import io.durbs.movieratings.model.convenience.ViewableRating
import io.durbs.movieratings.services.MovieService
import org.bson.Document
import org.bson.types.ObjectId
import ratpack.groovy.handling.GroovyChainAction
import ratpack.jackson.Jackson
import rx.functions.Func1

import static com.mongodb.client.model.Filters.text

import static ratpack.jackson.Jackson.fromJson

@Singleton
@Slf4j
class MovieRestEndpoint extends GroovyChainAction {

  @Inject
  MovieService movieService

  @Inject
  APIConfig apiConfig

  @Override
  void execute() throws Exception {

    get('movies/search') {

      final String queryTerm = request.queryParams.get('q', '')

      movieService
        .getAllMovies(text(queryTerm), new PaginationSupport(context, apiConfig))
        .toList()
        .defaultIfEmpty([])
        .subscribe { final List<RatedMovie> movies ->

        render Jackson.json(movies)
      }
    }

    path('movies') {

      byMethod {

        get {

          movieService
            .getAllMovies(new Document(), new PaginationSupport(context, apiConfig))
            .toList()
            .defaultIfEmpty([])
            .subscribe { final List<RatedMovie> movies ->

            render Jackson.json(movies)
          }
        }

        post {

          context.get(JWTTokenHandler)

          context.parse(fromJson(Movie))
            .observe()
            .flatMap({ final Movie movie ->

            movieService.createMovie(movie)
          } as Func1)
            .subscribe { final String movieID ->

            redirect("/api/movie/${movieID}")
          }
        }
      }
    }

    prefix('movies/:id') {

      all {

        final String movieId = context.pathTokens.get('id')

        if (ObjectId.isValid(movieId)) {

          next(single(new ObjectId(movieId)))
        } else {

          log.debug("Request path does not contain a valid ObjectId '${movieId}'")
          context.clientError(404)
        }
      }

      path { final ObjectId movieId ->

        byMethod {

          get {

            movieService
              .getMovie(movieId)
              .single()
              .doOnError({

              context.clientError(404)
            })
              .subscribe { final Movie movie ->

              render Jackson.json(movie)
            }
          }

          put {

            context.get(JWTTokenHandler)

            context.parse(fromJson(Movie))
              .observe()
              .flatMap({ final Movie movie ->

              render(Jackson.json('things'))
            })

          }

          delete {

            context.get(JWTTokenHandler)

            movieService
              .deleteMovieByID(movieId)
              .single()
              .subscribe { final Boolean success ->

              redirect('/api/movies')
            }
          }
        }
      }

      path('ratings') { final ObjectId movieId ->

        byMethod {

          get {

            movieService
              .getMovieRatings(movieId)
              .toList()
              .subscribe { final List<ViewableRating> ratings ->

              render Jackson.json(ratings)
            }
          }

          post {

            context.get(JWTTokenHandler)

            context.parse(fromJson(Rating))
              .observe()
              .flatMap({ final Rating rating ->

              final Range<Integer> acceptableRatingRange = Range.closed(apiConfig.ratingLowerBound, apiConfig.ratingUpperBound)
              if (!acceptableRatingRange.contains(rating.rating)) {

                throw new IllegalArgumentException("A range must be between ${apiConfig.ratingLowerBound} and ${apiConfig.ratingUpperBound}")
              }

              rating.movieId = movieId
              rating.userId = context.get(User).id

              movieService.rateMovie(rating)
            } as Func1)
              .subscribe { final Rating rating ->

              redirect("/api/movie/${rating.movieId.toString()}")
            }
          }
        }
      }
    }
  }
}
