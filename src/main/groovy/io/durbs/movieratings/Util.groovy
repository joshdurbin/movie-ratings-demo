package io.durbs.movieratings

import com.google.inject.Singleton
import groovy.transform.CompileStatic
import io.durbs.movieratings.model.persistent.Movie
import io.durbs.movieratings.model.persistent.User
import org.bson.types.ObjectId

@CompileStatic
@Singleton
class Util {

  public static final String getRedisUserKey(final User user) {

    "${Constants.REDIS_USER_KEY_PREFIX}-${user.id.toString()}"
  }

  public static final String getRedisMovieKey(final Movie movie) {
    getRedisMovieKey(movie.id)
  }

  public static final String getRedisMovieKey(final ObjectId objectId) {
    "${Constants.REDIS_MOVIE_KEY_PREFIX}-${objectId.toString()}"
  }

}
