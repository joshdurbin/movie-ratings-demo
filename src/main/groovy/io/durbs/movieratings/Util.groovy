package io.durbs.movieratings

import com.google.inject.Singleton
import groovy.transform.CompileStatic
import io.durbs.movieratings.model.Movie
import io.durbs.movieratings.model.User

@CompileStatic
@Singleton
class Util {

  public static final String getRedisUserKey(final User user) {

    "${Constants.REDIS_USER_KEY_PREFIX}-${user.id.toString()}"
  }

  public static final String getRedisMovieKey(final Movie movie) {
    "${Constants.REDIS_MOVIE_KEY_PREFIX}-${movie.id.toString()}"
  }

}
