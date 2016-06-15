package io.durbs.movieratings.services

import com.google.inject.Inject
import com.google.inject.Singleton
import com.lambdaworks.redis.SetArgs
import com.lambdaworks.redis.api.rx.RedisReactiveCommands
import com.mongodb.client.model.Updates
import com.mongodb.rx.client.MongoDatabase
import com.mongodb.rx.client.Success
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.Constants
import io.durbs.movieratings.MovieRatingsConfig
import io.durbs.movieratings.codec.mongo.RatingCodec
import io.durbs.movieratings.model.ComputedUserRating
import io.durbs.movieratings.model.ExternalRating
import io.durbs.movieratings.model.Rating

import org.bson.Document
import org.bson.types.ObjectId
import rx.Observable
import rx.functions.Action1
import rx.functions.Func2

import static com.mongodb.client.model.Accumulators.avg
import static com.mongodb.client.model.Accumulators.sum
import static com.mongodb.client.model.Aggregates.group
import static com.mongodb.client.model.Aggregates.match
import static com.mongodb.client.model.Filters.and
import static com.mongodb.client.model.Filters.eq

@CompileStatic
@Singleton
@Slf4j
class RatingService {

  @Inject
  RedisReactiveCommands<String, ComputedUserRating> computedRatingCache

  @Inject
  RedisReactiveCommands<String, ExternalRating> externalRatingCache

  @Inject
  MovieRatingsConfig movieRatingsConfig

  @Inject
  MongoDatabase mongoDatabase

  @Inject
  OMDBService omdbService

  static String getComputedUserRatingCacheKey(final ObjectId movieId) {

    "${Constants.REDIS_COMPUTED_USER_RATING_PREFIX}-${movieId.toString()}"
  }

  static String getExternalRatingCacheKey(final String imdbId) {

    "${Constants.REDIS_EXTERNAL_RATING_PREFIX}-${imdbId}"
  }

  Observable<ComputedUserRating> getComputedUserRating(final ObjectId movieId) {

    computedRatingCache.get(getComputedUserRatingCacheKey(movieId))
      .bindExec()
      .switchIfEmpty(

      mongoDatabase.getCollection(RatingCodec.COLLECTION_NAME, Rating)
        .aggregate(Arrays.asList(
        match(eq(RatingCodec.MOVIE_ID_PROPERTY, movieId)),
        group('$movieId', Arrays.asList(avg('averageRating', '$rating'), sum('numberOfRatings', 1)))))
        .toObservable()
        .map({ final Document document ->

        final Double rating = document.getDouble('averageRating')
        final Integer totalRatings = document.getInteger('numberOfRatings')
        final Double scaledRating = new BigDecimal(rating).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue()

        log.debug("Computed a rating of ${scaledRating} out of ${totalRatings} ratings for the movie ID ${movieId.toString()}")
        new ComputedUserRating(rating: scaledRating, totalRatings: totalRatings)
      })
        .defaultIfEmpty(new ComputedUserRating(totalRatings: 0))
        .doOnNext { final ComputedUserRating computedRating ->

        log.debug("Inserted computed rating in cache as key ${getComputedUserRatingCacheKey(movieId)}")
        computedRatingCache.set(getComputedUserRatingCacheKey(movieId),
          computedRating,
          SetArgs.Builder.ex(movieRatingsConfig.computedRatingsCacheTTLInSeconds)).subscribe()
      }
    )
  }

  Observable<ExternalRating> getExternalRating(final String imdbId) {

    externalRatingCache.get(getExternalRatingCacheKey(imdbId))
      .bindExec()
      .switchIfEmpty (
      omdbService.getOMDBExternalRatingForMovie(imdbId)
        .doOnNext { final ExternalRating externalRating ->

        log.info("Inserted external rating in cache as key ${getExternalRatingCacheKey(imdbId)}")
        externalRatingCache.set(getExternalRatingCacheKey(imdbId),
          externalRating,
          SetArgs.Builder.ex(movieRatingsConfig.externalRatingsCacheTTLInSeconds)).subscribe()
      })
  }

  Observable<Rating> rateMovie(final Rating rating) {

    mongoDatabase.getCollection(RatingCodec.COLLECTION_NAME, Rating)
      .findOneAndUpdate(and(eq(RatingCodec.MOVIE_ID_PROPERTY, rating.movieId), eq(RatingCodec.USER_ID_PROPERTY, rating.userId)),
      Updates.set(RatingCodec.RATING_PROPERTY, rating.rating))
      .asObservable()
      .bindExec()
      .switchIfEmpty(
      mongoDatabase.getCollection(RatingCodec.COLLECTION_NAME, Rating)
        .insertOne(rating)
        .map { final Success success ->

        rating
      }
    )
      .asObservable()
      .doOnNext({ final Rating observedRating ->

      log.debug("Clearing computed rating cache with key ${getComputedUserRatingCacheKey(observedRating.movieId)}")
      computedRatingCache.del(getComputedUserRatingCacheKey(observedRating.movieId)).subscribe()
    } as Action1)
      .bindExec()
  }

  Observable<Long> deleteRatingsForMovieId(final ObjectId movieObjectId) {

    final Observable<Long> deletedComputedRatingsFromCache = computedRatingCache.del(movieObjectId.toString())
    final Observable<Long> deleteExternalRatingsFromCache = externalRatingCache.del(movieObjectId.toString())

    Observable.zip(deletedComputedRatingsFromCache, deleteExternalRatingsFromCache,
      { final Long numberOfComputedRatingsDeletedFromCache,
        final Long numberOfExternalRatingsDeletedFromCache ->

        numberOfComputedRatingsDeletedFromCache + numberOfExternalRatingsDeletedFromCache
      } as Func2)
      .bindExec()
  }
}