package io.durbs.movieratings

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class Constants {

  public static final String PAGE_SIZE_QUERY_PARAM_KEY = 'pageSize'
  public static final String PAGE_NUMBER_QUERY_PARAM_KEY = 'pageNumber'

  public static final String BEARER_AUTHORIZATION_SCHEMA_KEY = 'Bearer'
  public static final String BEARER_AUTHOIRZATION_PREFIX = "${BEARER_AUTHORIZATION_SCHEMA_KEY} "

  public static final String REDIS_USER_KEY_PREFIX = 'user'
  public static final String REDIS_MOVIE_KEY_PREFIX = 'movie'

  public static final String JWT_USER_OBJECT_ID_CLAIM = 'userId'

  public static final String PLACE_HOLDER_INVALID_JWT_TOKEN = '--'

  public static final Integer DEFAULT_RATING = 0
}
