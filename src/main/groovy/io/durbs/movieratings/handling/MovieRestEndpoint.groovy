package io.durbs.movieratings.handling

import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.util.logging.Slf4j
import io.durbs.movieratings.handling.auth.JWTTokenHandler
import io.durbs.movieratings.model.Movie
import io.durbs.movieratings.model.RatedMovie
import io.durbs.movieratings.model.ViewableRating
import io.durbs.movieratings.services.MovieService
import org.bson.types.ObjectId
import ratpack.groovy.handling.GroovyChainAction
import ratpack.jackson.Jackson
import rx.functions.Func1

import static ratpack.jackson.Jackson.fromJson

@Singleton
@Slf4j
class MovieRestEndpoint extends GroovyChainAction {

  @Inject
  MovieService movieService

  @Override
  void execute() throws Exception {

    get('movies') {

      movieService
        .getAllMovies()
        .toList()
        .defaultIfEmpty([])
        .subscribe { final List<RatedMovie> movies ->

        render Jackson.json(movies)
      }
    }

    get('movie/:id') {

      movieService
        .getMovie(new ObjectId(pathTokens.get('id')))
        .single()
        .subscribe { final Movie movie ->

        render Jackson.json(movie)
      }
    }

    get('movie/:id/ratings') {

      movieService
        .getMovieRatings(new ObjectId(pathTokens.get('id')))
        .toList()
        .subscribe { final List<ViewableRating> ratings ->

        render Jackson.json(ratings)
      }
    }

    all(JWTTokenHandler)

    post('movie') {

      context.parse(fromJson(Movie))
        .observe()
        .flatMap({ final Movie movie ->

        movieService.createMovie(movie)
      } as Func1)
      .subscribe { final String movieID ->

        redirect("/api/movie/${movieID}")
      }
    }

    path('movie/:id') {

      final String movieID = pathTokens.get('id')

      byMethod {

//        put {
//
//        }

        delete {

          movieService
            .deleteMovieByID(new ObjectId(movieID))
            .single()
            .subscribe { final Boolean success ->

            redirect('/api/movies')
          }
        }
      }
    }
  }
}
