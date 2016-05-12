package io.durbs.movieratings.model.persistent

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import io.durbs.movieratings.Constants

@Canonical
@CompileStatic
class RatedMovie extends Movie {

  Double rating

  Boolean isRated() {

    rating && rating != Constants.DEFAULT_RATING
  }
}
