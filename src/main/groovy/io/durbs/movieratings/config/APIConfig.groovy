package io.durbs.movieratings.config

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
@Immutable
class APIConfig {

  static String CONFIG_ROOT = '/api'

  Integer ratingLowerBound
  Integer ratingUpperBound
}
