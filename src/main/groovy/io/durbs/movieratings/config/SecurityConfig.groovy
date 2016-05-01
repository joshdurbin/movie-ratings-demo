package io.durbs.movieratings.config

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@Immutable
@CompileStatic
class SecurityConfig {

  static String CONFIG_ROOT = '/security'

  String jwtSigningKey
  Long jwtTokenTTLInHours
}
