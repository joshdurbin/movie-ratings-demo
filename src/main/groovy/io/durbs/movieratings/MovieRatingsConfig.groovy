package io.durbs.movieratings

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
@Immutable
class MovieRatingsConfig {

  Integer defaultResultsPageSize
  Integer maxResultsPageSize
  Integer defaultFirstPage

  String mongoDb
  String mongoURI

  String jwtSigningKey
  Long jwtTokenTTLInHours

  String redisURI
  Long computedRatingsCacheTTLInSeconds
  Long externalRatingsCacheTTLInSeconds
  Long userCacheTTLInSeconds
}
