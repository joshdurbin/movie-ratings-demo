package io.durbs.movieratings.services

import com.google.inject.Inject
import com.google.inject.Singleton
import com.mongodb.DBCollection
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Updates
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.rx.client.MongoDatabase
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.codec.mongo.MovieCodec
import io.durbs.movieratings.codec.mongo.RatingCodec
import io.durbs.movieratings.model.ComputedUserRating
import io.durbs.movieratings.model.ExternalRating
import io.durbs.movieratings.model.Movie
import io.durbs.movieratings.model.Rating
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import rx.Observable
import rx.functions.Func1
import rx.functions.Func2
import rx.functions.Func3

import static com.mongodb.client.model.Filters.eq

@CompileStatic
@Singleton
@Slf4j
class MovieService {

  @Inject
  MongoDatabase mongoDatabase

  @Inject
  RatingService ratingService

  private final Func1 MOVIE_TO_RATED_MOVIE = { final Movie movie ->

    Observable.zip(ratingService.getComputedUserRating(movie.id),
      ratingService.getExternalRating(movie.imdbId),
      { final ComputedUserRating computedUserRating,
        final ExternalRating externalRating ->

        movie.computedRating = computedUserRating
        movie.externalRating = externalRating

        movie
      } as Func2)
  } as Func1

  Observable<Boolean> movieExists(final ObjectId objectId) {

    mongoDatabase.getCollection(MovieCodec.COLLECTION_NAME)
      .find(eq(DBCollection.ID_FIELD_NAME, objectId))
      .projection(Projections.include(DBCollection.ID_FIELD_NAME))
      .toObservable()
      .isEmpty()
      .map({ final Boolean movieDoesNotExist -> !movieDoesNotExist })
      .bindExec()
  }

  Observable<Movie> getMovie(final ObjectId objectId) {

    mongoDatabase.getCollection(MovieCodec.COLLECTION_NAME, Movie)
      .find(eq(DBCollection.ID_FIELD_NAME, objectId))
      .toObservable()
      .flatMap(MOVIE_TO_RATED_MOVIE).bindExec()
  }

  Observable<String> getExistingMovieIMDBIDs() {

    mongoDatabase.getCollection(MovieCodec.COLLECTION_NAME)
      .find()
      .projection(Projections.include(MovieCodec.IMDB_ID_PROPERTY))
      .toObservable()
      .map({ final Document document ->

        document.getString(MovieCodec.IMDB_ID_PROPERTY)
      }).bindExec()
  }

  Observable<UpdateResult> updateMovie(final Movie movie) {

    mongoDatabase.getCollection(MovieCodec.COLLECTION_NAME, Movie)
      .updateOne(eq(DBCollection.ID_FIELD_NAME, movie.id),
      Updates.combine(
        Updates.set(MovieCodec.NAME_PROPERTY, movie.name),
        Updates.set(MovieCodec.IMDB_ID_PROPERTY, movie.imdbId),
        Updates.set(MovieCodec.DESCRIPTION_PROPERTY, movie.name),
        Updates.set(MovieCodec.POSTER_IMAGE_URI_PROPERTY, movie.posterImageURI),
        Updates.set(MovieCodec.YEAR_RELEASED_PROPERTY, movie.yearReleased)))
      .bindExec()
  }

  Observable<String> createMovie(final Movie movie) {

    mongoDatabase.getCollection(MovieCodec.COLLECTION_NAME, Movie)
      .insertOne(movie)
      .map {

      movie.id.toString()
    }
    .bindExec()
  }

  Observable<Boolean> deleteMovieByID(final ObjectId movieObjectId) {

    final Observable<Long> deleteRatingsFromCache = ratingService.deleteRatingsForMovieId(movieObjectId)
    final Observable<DeleteResult> deleteMovie = mongoDatabase.getCollection(MovieCodec.COLLECTION_NAME, Movie)
      .deleteOne(eq(DBCollection.ID_FIELD_NAME, movieObjectId))
    final Observable<DeleteResult> deleteMovieRatings = mongoDatabase.getCollection(RatingCodec.COLLECTION_NAME, Rating)
      .deleteMany(eq(RatingCodec.MOVIE_ID_PROPERTY, movieObjectId))

    Observable.zip(deleteRatingsFromCache, deleteMovie, deleteMovieRatings,
      { final Long deletedRedisKeys,
        final DeleteResult deletedMovieResult,
        final DeleteResult deletedMovieRatingsResult ->

        log.info("Removing movie id ${movieObjectId.toString()}, deleting ${deletedRedisKeys} redis keys, ${deletedMovieResult.deletedCount} movies, and ${deletedMovieRatingsResult.deletedCount} ratings...")

        deletedMovieResult.deletedCount > 0

      } as Func3)
      .bindExec()
  }

  Observable<Movie> getAllMovies(final Bson queryFilter) {

    mongoDatabase.getCollection(MovieCodec.COLLECTION_NAME, Movie)
      .find()
      .filter(queryFilter)
      .toObservable()
      .bindExec()
      .flatMap(MOVIE_TO_RATED_MOVIE)
  }
}
