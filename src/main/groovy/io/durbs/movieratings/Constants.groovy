package io.durbs.movieratings

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class Constants {

  public static final String BEARER_AUTHORIZATION_SCHEMA_KEY = 'Bearer'
  public static final String BEARER_AUTHOIRZATION_PREFIX = "${BEARER_AUTHORIZATION_SCHEMA_KEY} "

  public static final String REDIS_USER_KEY_PREFIX = 'user'
  public static final String REDIS_MOVIE_KEY_PREFIX = 'movie'

  public static final String JWT_USER_OBJECT_ID_CLAIM = 'userID'

  public static final String PLACE_HOLDER_INVALID_JWT_TOKEN = '--'
}
