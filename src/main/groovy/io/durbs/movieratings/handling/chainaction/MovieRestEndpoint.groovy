package io.durbs.movieratings.handling.chainaction

import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.util.logging.Slf4j
import io.durbs.movieratings.handling.handler.JWTTokenHandler
import io.durbs.movieratings.handling.handler.MovieIDExtractionAndVerificationHandler
import io.durbs.movieratings.model.Movie
import io.durbs.movieratings.model.Rating
import io.durbs.movieratings.model.User
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

    get('movies/imdbids') {

      movieService
        .getExistingMovieIMDBIDs()
        .toList()
        .defaultIfEmpty([])
        .subscribe { final List<String> movieIMDBIds ->

        render Jackson.json(movieIMDBIds)
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

          final String imdbId = context.request.queryParams.get('imdbId')

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
