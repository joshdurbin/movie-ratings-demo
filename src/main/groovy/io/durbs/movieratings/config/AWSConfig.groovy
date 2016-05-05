package io.durbs.movieratings.config

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@Immutable
@CompileStatic
class AWSConfig {

  static String CONFIG_ROOT = '/aws'

  String accessKey
  String secretKey
}
