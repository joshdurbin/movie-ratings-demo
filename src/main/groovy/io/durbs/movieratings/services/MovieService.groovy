package io.durbs.movieratings.services

import com.google.inject.Inject
import com.google.inject.Singleton
import com.lambdaworks.redis.SetArgs
import com.lambdaworks.redis.api.rx.RedisReactiveCommands
import com.mongodb.DBCollection
import com.mongodb.client.result.DeleteResult
import com.mongodb.rx.client.MongoDatabase
import groovy.util.logging.Slf4j
import io.durbs.movieratings.Util
import io.durbs.movieratings.codec.mongo.MovieCodec
import io.durbs.movieratings.codec.mongo.RatingCodec
import io.durbs.movieratings.codec.mongo.UserCodec
import io.durbs.movieratings.config.RedisConfig
import io.durbs.movieratings.model.Movie
import io.durbs.movieratings.model.RatedMovie
import io.durbs.movieratings.model.Rating
import io.durbs.movieratings.model.User
import io.durbs.movieratings.model.ViewableRating
import org.bson.types.ObjectId
import rx.Observable
import rx.functions.Func1
import rx.functions.Func3
import rx.observables.MathObservable

import static com.mongodb.client.model.Filters.eq

@Singleton
@Slf4j
class MovieService {

  @Inject
  MongoDatabase mongoDatabase

  @Inject
  RedisReactiveCommands<String, RatedMovie> ratedMovieRedisReactiveCommands

  @Inject
  RedisConfig redisConfig

  Observable<Movie> getMovie(final ObjectId objectId) {

    mongoDatabase.getCollection(MovieCodec.COLLETION_NAME, Movie)
      .find(eq(DBCollection.ID_FIELD_NAME, objectId))
      .toObservable()
      .bindExec()
  }

  Observable<ViewableRating> getMovieRatings(final ObjectId objectId) {

    mongoDatabase.getCollection(RatingCodec.COLLETION_NAME, Rating)
      .find(eq(RatingCodec.MOVIE_ID_PROPERTY, objectId))
      .toObservable()
      .flatMap({ final Rating rating ->

        mongoDatabase.getCollection(UserCodec.COLLETION_NAME, User)
          .find(eq(DBCollection.ID_FIELD_NAME, rating.userID))
          .toObservable()
          .map({ final User user ->

          new ViewableRating(username: user.username, usersGivenName: user.name, rating: rating.rating, comment: rating.comment)
        })
      } as Func1)
      .bindExec()
  }

  Observable<String> createMovie(final Movie movie) {

    mongoDatabase.getCollection(MovieCodec.COLLETION_NAME, Movie)
      .insertOne(movie)
      .map {

        movie.id.toString()
      }
      .bindExec()
  }

  Observable<Boolean> deleteMovieByID(final ObjectId movieObjectId) {

    final Observable<Long> deleteMovieRatingCache = ratedMovieRedisReactiveCommands.del(movieObjectId.toString())
    final Observable<DeleteResult> deleteMovie = mongoDatabase.getCollection(MovieCodec.COLLETION_NAME, Movie)
      .deleteOne(eq(DBCollection.ID_FIELD_NAME, movieObjectId))
    final Observable<DeleteResult> deleteMovieRatings = mongoDatabase.getCollection(RatingCodec.COLLETION_NAME, Rating)
      .deleteMany(eq(RatingCodec.MOVIE_ID_PROPERTY, movieObjectId))

    Observable.zip(deleteMovieRatingCache, deleteMovie, deleteMovieRatings, { Long deletedRedisKeys, DeleteResult deletedMovieResult, DeleteResult deletedMovieRatingsResult ->

      log.info("Removing movie id ${movieObjectId.toString()}, deleting ${deletedRedisKeys} redis keys, ${deletedMovieResult.deletedCount} movies, and ${deletedMovieRatingsResult.deletedCount} ratings...")

      true
    } as Func3)
    .bindExec()
  }

//  Observable<Boolean> rateMovie(final ObjectId movieID, final ObjectId userID, final Double rating) {
//
//    mongoDatabase.getCollection(RatingCodec.COLLETION_NAME, Rating)
//      .find(and(eq(RatingCodec.MOVIE_ID_PROPERTY, movieID), eq(RatingCodec.USER_ID_PROPERTY, userID)))
//      .toObservable()
//      .
//  }

  Observable<RatedMovie> getAllMovies() {

    mongoDatabase.getCollection(MovieCodec.COLLETION_NAME, Movie)
      .find()
      .toObservable()
      .flatMap({ final Movie movie ->

      ratedMovieRedisReactiveCommands.get(movie.id.toString())
        .switchIfEmpty(

        MathObservable.averageDouble(mongoDatabase.getCollection(RatingCodec.COLLETION_NAME, Rating)
          .find(eq(RatingCodec.MOVIE_ID_PROPERTY, movie.id))
          .toObservable()
          .map({ final Rating rating ->

          rating.rating
        } as Func1))
          .map({ final Double rating ->

          new RatedMovie(

            id: movie.id,
            name: movie.name,
            description: movie.description,
            imageURI: movie.imageURI,
            rating: rating
          )
        })
        .doOnNext { final RatedMovie ratedMovie ->

          ratedMovieRedisReactiveCommands.set(Util.getRedisMovieKey(ratedMovie), ratedMovie, SetArgs.Builder.ex(redisConfig.movieRatingsCacheTTLInSeconds)).subscribe()
        }
      )
    } as Func1)
    .bindExec()
  }
}
