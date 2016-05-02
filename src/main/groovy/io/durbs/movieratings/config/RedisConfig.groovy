package io.durbs.movieratings.config

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@Immutable
@CompileStatic
class RedisConfig {

  static String CONFIG_ROOT = '/redis'

  String uri
  Long cacheTTLInSeconds
}
