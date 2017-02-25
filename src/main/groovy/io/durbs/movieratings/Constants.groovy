package io.durbs.movieratings

import groovy.transform.CompileStatic
import org.bson.Document

@CompileStatic
@Singleton
class Constants {

  public static String EMPTY_STRING = ''

  public static Document EMPTY_DOCUMENT = new Document()

  public static String PAGE_SIZE_QUERY_PARAM_KEY = 'pageSize'
  public static String PAGE_NUMBER_QUERY_PARAM_KEY = 'pageNumber'

  public static String OMDB_DEFAULT_DELIMITER = ','

  public static String BEARER_AUTHORIZATION_SCHEMA_KEY = 'Bearer'
  public static String BEARER_AUTHORIZATION_PREFIX = "${BEARER_AUTHORIZATION_SCHEMA_KEY} "

  public static String REDIS_COMPUTED_USER_RATING_PREFIX = 'computed-user-rating'
  public static String REDIS_EXTERNAL_RATING_PREFIX = 'external-rating'
  public static String REDIS_USER_KEY_PREFIX = 'user'

  public static String JWT_USER_OBJECT_ID_CLAIM = 'userId'

  public static String PLACE_HOLDER_INVALID_JWT_TOKEN = '--'

  public static Integer DEFAULT_KRYO_BUFFER = 1024 * 100
}
