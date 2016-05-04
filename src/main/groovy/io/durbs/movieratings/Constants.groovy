package io.durbs.movieratings

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class Constants {

  public static final String JWT_USER_OBJECT_ID_CLAIM = 'userID'

  public static final String PLACE_HOLDER_INVALID_JWT_TOKEN = '--'
}
