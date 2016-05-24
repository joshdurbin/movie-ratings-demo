package io.durbs.movieratings

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class Constants {

  public static final String BEARER_AUTHORIZATION_SCHEMA_KEY = 'Bearer'
  public static final String BEARER_AUTHORIZATION_PREFIX = "${BEARER_AUTHORIZATION_SCHEMA_KEY} "

  public static final String REDIS_COMPUTED_USER_RATING_PREFIX = 'computed-user-rating'
  public static final String REDIS_EXTERNAL_RATING_PREFIX = 'external-rating'
  public static final String REDIS_USER_KEY_PREFIX = 'user'

  public static final String JWT_USER_OBJECT_ID_CLAIM = 'userId'

  public static final String PLACE_HOLDER_INVALID_JWT_TOKEN = '--'

  public static final Integer DEFAULT_KRYO_BUFFER = 1024 * 100
}
