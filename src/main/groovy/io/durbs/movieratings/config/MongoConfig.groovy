package io.durbs.movieratings.config

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@Immutable
@CompileStatic
class MongoConfig {

  static String CONFIG_ROOT = '/mongo'

  String db
  String uri
}
