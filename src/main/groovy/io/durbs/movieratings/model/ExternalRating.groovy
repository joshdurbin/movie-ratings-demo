package io.durbs.movieratings.model

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@Immutable
@CompileStatic
class ExternalRating {

  Double imdbRating
  Integer totalImdbRatings

  Double rottenTomatoRating
  Integer totalRottenTomatoRatings

  Double rottenTomatoUserRating
  Integer totalRottenTomatoUserRatings

  Integer metascore
}
