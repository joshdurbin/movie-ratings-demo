package io.durbs.movieratings.model

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@Immutable
@CompileStatic
class ComputedUserRating {

  Double rating
  Integer totalRatings
}
