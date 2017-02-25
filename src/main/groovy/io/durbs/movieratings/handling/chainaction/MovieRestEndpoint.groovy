package io.durbs.movieratings.handling.chainaction

import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.util.logging.Slf4j
import io.durbs.movieratings.handling.handler.pagination.GetAllMoviesHandler
import io.durbs.movieratings.handling.handler.JWTTokenHandler
import io.durbs.movieratings.handling.handler.MovieIDExtractionAndVerificationHandler
import io.durbs.movieratings.handling.handler.pagination.MovieSearchHandler
import io.durbs.movieratings.model.Movie
import io.durbs.movieratings.model.Rating
import io.durbs.movieratings.model.User
import io.durbs.movieratings.model.ViewableRating
import io.durbs.movieratings.services.MovieService
import io.durbs.movieratings.services.OMDBService
import io.durbs.movieratings.services.RatingService
import org.bson.types.ObjectId
import ratpack.groovy.handling.GroovyChainAction
import ratpack.handling.Context
import ratpack.jackson.Jackson
import rx.functions.Func1

import javax.validation.ConstraintViolation
import javax.validation.Validator

import static ratpack.jackson.Jackson.fromJson

@Singleton
@Slf4j
class MovieRestEndpoint extends GroovyChainAction {

  @Inject
  MovieService movieService

  @Inject
  RatingService ratingService

  @Inject
  OMDBService omdbService

  @Inject
  Validator validator

  @Override
  void execute() throws Exception {

    onlyIf({
      Context context ->
        !context.getRequest().getMethod().isGet()
    }, JWTTokenHandler)

    get('movies/search', MovieSearchHandler)

    get('movies/imdbids') {

      movieService
        .getExistingMovieIMDBIDs()
        .toList()
        .defaultIfEmpty([])
        .subscribe { List<String> movieIMDBIds ->

        render Jackson.json(movieIMDBIds)
      }
    }

    path('movies') {

      byMethod {

        get {
          insert context.get(GetAllMoviesHandler)
        }

        post {

          String imdbId = context.request.queryParams.get('imdbId')

          omdbService
            .getOMDBMovie(imdbId)
            .flatMap({ Movie movie ->

            movieService.createMovie(movie)
          } as Func1)
            .single()
            .subscribe { String movieID ->

            redirect("/api/movies/${movieID}")
          }
        }
      }
    }

    prefix('movies/:id') {

      all(MovieIDExtractionAndVerificationHandler)

      path { ObjectId movieId ->

        byMethod {

          get {

            movieService
              .getMovie(movieId)
              .single()
              .subscribe { Movie movie ->

              render Jackson.json(movie)
            }
          }

          delete {

            movieService
              .deleteMovieByID(movieId)
              .single()
              .subscribe { Boolean success ->

              redirect('/api/movies')
            }
          }
        }
      }

      path('ratings') { ObjectId movieId ->

        byMethod {

          get {

            ratingService
              .getIndividualUserRatingsAndComment(movieId)
              .toList()
              .subscribe { List<ViewableRating> ratings ->

              render Jackson.json(ratings)
            }
          }

          post {

            context.parse(fromJson(Rating))
              .observe()
              .flatMap({ Rating rating ->

              Set<ConstraintViolation<Rating>> violations = validator.validate(rating)
              if (!violations.empty) {
                throw new IllegalArgumentException("There are a number of constraint violations on rating submission... ${violations}")
              }

              rating.movieId = movieId
              rating.userId = context.get(User).id

              ratingService.rateMovie(rating)
            } as Func1)
              .subscribe { Rating rating ->

              redirect("/api/movies/${rating.movieId.toString()}")
            }
          }
        }
      }
    }
  }
}
