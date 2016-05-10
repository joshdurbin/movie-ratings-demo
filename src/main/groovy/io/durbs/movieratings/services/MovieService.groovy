package io.durbs.movieratings.services

import com.google.inject.Inject
import com.google.inject.Singleton
import com.lambdaworks.redis.SetArgs
import com.lambdaworks.redis.api.rx.RedisReactiveCommands
import com.mongodb.DBCollection
import com.mongodb.client.model.CountOptions
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import com.mongodb.client.result.DeleteResult
import com.mongodb.rx.client.MongoDatabase
import com.mongodb.rx.client.Success
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.Constants
import io.durbs.movieratings.PaginationSupport
import io.durbs.movieratings.Util
import io.durbs.movieratings.codec.mongo.MovieCodec
import io.durbs.movieratings.codec.mongo.RatingCodec
import io.durbs.movieratings.codec.mongo.UserCodec
import io.durbs.movieratings.config.RedisConfig
import io.durbs.movieratings.model.persistent.Movie
import io.durbs.movieratings.model.persistent.RatedMovie
import io.durbs.movieratings.model.persistent.Rating
import io.durbs.movieratings.model.persistent.User
import io.durbs.movieratings.model.convenience.ViewableRating
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import rx.Observable
import rx.functions.Action1
import rx.functions.Func1
import rx.functions.Func3
import rx.observables.MathObservable

import static com.mongodb.client.model.Filters.and
import static com.mongodb.client.model.Filters.eq

@CompileStatic
@Singleton
@Slf4j
class MovieService {

  @Inject
  MongoDatabase mongoDatabase

  @Inject
  RedisReactiveCommands<String, RatedMovie> ratedMovieRedisReactiveCommands

  @Inject
  RedisConfig redisConfig

  private final Func1 MOVIE_TO_RATED_MOVIE = { final Movie movie ->

    ratedMovieRedisReactiveCommands.get(movie.id.toString())
      .switchIfEmpty(

      MathObservable.averageInteger(mongoDatabase.getCollection(RatingCodec.COLLETION_NAME, Rating)
        .find(eq(RatingCodec.MOVIE_ID_PROPERTY, movie.id))
        .toObservable()
        .map({ final Rating rating ->

        rating.rating
      } as Func1)
        .defaultIfEmpty(Constants.DEFAULT_RATING))
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
  } as Func1

  Observable<Long> getTotalMovieCount(final Bson queryFilter) {

    mongoDatabase.getCollection(MovieCodec.COLLETION_NAME)
      .count(queryFilter, new CountOptions())
  }

  Observable<Boolean> movieExists(final ObjectId objectId) {

    mongoDatabase.getCollection(MovieCodec.COLLETION_NAME)
      .find(eq(DBCollection.ID_FIELD_NAME, objectId))
      .projection(Projections.include(DBCollection.ID_FIELD_NAME))
      .toObservable()
      .isEmpty()
      .map({ final Boolean movieDoesNotExist -> !movieDoesNotExist })
      .bindExec()
  }

  Observable<Movie> getMovie(final ObjectId objectId) {

    mongoDatabase.getCollection(MovieCodec.COLLETION_NAME, Movie)
      .find(eq(DBCollection.ID_FIELD_NAME, objectId))
      .toObservable()
      .bindExec()
  }

  Observable<RatedMovie> getRatedMovie(final ObjectId objectId) {

    mongoDatabase.getCollection(MovieCodec.COLLETION_NAME, Movie)
      .find(eq(DBCollection.ID_FIELD_NAME, objectId))
      .toObservable()
      .flatMap(MOVIE_TO_RATED_MOVIE)
      .bindExec()
  }

  Observable<ViewableRating> getMovieRatings(final ObjectId objectId) {

    mongoDatabase.getCollection(RatingCodec.COLLETION_NAME, Rating)
      .find(eq(RatingCodec.MOVIE_ID_PROPERTY, objectId))
      .toObservable()
      .flatMap({ final Rating rating ->

        mongoDatabase.getCollection(UserCodec.COLLETION_NAME, User)
          .find(eq(DBCollection.ID_FIELD_NAME, rating.userId))
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

  Observable<Rating> rateMovie(final Rating rating) {

    mongoDatabase.getCollection(RatingCodec.COLLETION_NAME, Rating)
      .findOneAndUpdate(and(eq(RatingCodec.MOVIE_ID_PROPERTY, rating.movieId), eq(RatingCodec.USER_ID_PROPERTY, rating.userId)),
        Updates.combine(
          Updates.set(RatingCodec.RATING_PROPERTY, rating.rating),
          Updates.set(RatingCodec.COMMENT_PROPERTY, rating.comment)))
      .asObservable()
      .switchIfEmpty(
        mongoDatabase.getCollection(RatingCodec.COLLETION_NAME, Rating)
          .insertOne(rating)
          .map { final Success success ->

            rating
          }
      )
      .asObservable()
      .doOnNext ({ final Rating observedRating ->

      ratedMovieRedisReactiveCommands.del(Util.getRedisMovieKey(observedRating.movieId))
    } as Action1)
    .bindExec()
  }

  Observable<RatedMovie> getAllMovies(final Bson queryFilter, final PaginationSupport paginationSupport) {

    mongoDatabase.getCollection(MovieCodec.COLLETION_NAME, Movie)
      .find()
      .filter(queryFilter)
      .limit(paginationSupport.pageSize)
      .skip(paginationSupport.offSet)
      .toObservable()
      .flatMap(MOVIE_TO_RATED_MOVIE)
    .bindExec()
  }
}
