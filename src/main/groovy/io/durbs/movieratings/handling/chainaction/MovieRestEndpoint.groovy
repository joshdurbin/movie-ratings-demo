package io.durbs.movieratings.handling.chainaction

import com.google.common.collect.Range
import com.google.inject.Inject
import com.google.inject.Singleton
import com.mongodb.client.result.UpdateResult
import groovy.util.logging.Slf4j
import io.durbs.movieratings.MovieRatingsConfig
import io.durbs.movieratings.handling.handler.JWTTokenHandler
import io.durbs.movieratings.handling.handler.MovieIDExtractionAndVerificationHandler
import io.durbs.movieratings.model.Movie
import io.durbs.movieratings.model.Rating
import io.durbs.movieratings.model.User
import io.durbs.movieratings.model.ViewableRating
import io.durbs.movieratings.services.MovieService
import io.durbs.movieratings.services.OMDBService
import io.durbs.movieratings.services.RatingService
import org.bson.Document
import org.bson.types.ObjectId
import ratpack.groovy.handling.GroovyChainAction
import ratpack.handling.Context
import ratpack.jackson.Jackson
import rx.functions.Func1

import javax.validation.ConstraintViolation
import javax.validation.Validator

import static com.mongodb.client.model.Filters.text
import static ratpack.jackson.Jackson.fromJson

@Singleton
@Slf4j
class MovieRestEndpoint extends GroovyChainAction {

  @Inject
  MovieService movieService

  @Inject
  RatingService ratingService

  @Inject
  MovieRatingsConfig movieRatingsConfig

  @Inject
  OMDBService omdbService

  @Inject
  Validator validator

  @Override
  void execute() throws Exception {

    onlyIf({
      final Context context ->
        !context.getRequest().getMethod().isGet()
    }, JWTTokenHandler)

    get('movies/search') {

      final String queryTerm = request.queryParams.get('q', '')

      movieService
        .getAllMovies(text(queryTerm))
        .toList()
        .defaultIfEmpty([])
        .subscribe { final List<Movie> movies ->

        render Jackson.json(movies)
      }
    }

    get('movies/import/:imdbId') {

      final String imdbId = context.pathTokens.get('imdbId')

      omdbService
        .getOMDBMovie(imdbId)
        .flatMap({ final Movie movie ->

        movieService.createMovie(movie)
      } as Func1)
      .single()
      .subscribe { final String movieID ->

        redirect("/api/movies/${movieID}")
      }
    }

    path('movies') {

      byMethod {

        get {

          movieService
            .getAllMovies(new Document())
            .toList()
            .defaultIfEmpty([])
            .subscribe { final List<Movie> movies ->

            render Jackson.json(movies)
          }
        }

        post {

          context.parse(fromJson(Movie))
            .observe()
            .flatMap({ final Movie movie ->

            final Set<ConstraintViolation<Movie>> violations = validator.validate(movie)
            if (!violations.empty) {
              throw new IllegalArgumentException("There are a number of constraint violations while creating the movie ${violations}")
            }

            movieService.createMovie(movie)
          } as Func1)
            .subscribe { final String movieID ->

            redirect("/api/movies/${movieID}")
          }
        }
      }
    }

    prefix('movies/:id') {

      all(MovieIDExtractionAndVerificationHandler)

      path { final ObjectId movieId ->

        byMethod {

          get {

            movieService
              .getMovie(movieId)
              .single()
              .subscribe { final Movie movie ->

              render Jackson.json(movie)
            }
          }

          put {

            context.parse(fromJson(Movie))
              .observe()
              .flatMap({ final Movie movie ->

              final Set<ConstraintViolation<Movie>> violations = validator.validate(movie)
              if (!violations.empty) {
                throw new IllegalArgumentException("There are a number of constraint violations while updating the movie ${violations}")
              }

              movieService.updateMovie(movie)
            } as Func1)
              .subscribe { final UpdateResult updateResult ->

              redirect("/api/movies/${movieId.toString()}")
            }

          }

          delete {

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

            ratingService
              .getIndividualUserRatingsAndComment(movieId)
              .toList()
              .subscribe { final List<ViewableRating> ratings ->

              render Jackson.json(ratings)
            }
          }

          post {

            context.parse(fromJson(Rating))
              .observe()
              .flatMap({ final Rating rating ->

              final Set<ConstraintViolation<Rating>> violations = validator.validate(rating)
              if (!violations.empty) {
                throw new IllegalArgumentException("There are a number of constraint violations on rating submission... ${violations}")
              }

              rating.movieId = movieId
              rating.userId = context.get(User).id

              ratingService.rateMovie(rating)
            } as Func1)
              .subscribe { final Rating rating ->

              redirect("/api/movies/${rating.movieId.toString()}")
            }
          }
        }
      }
    }
  }
}
