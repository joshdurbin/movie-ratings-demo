package io.durbs.movieratings.model

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@Canonical
@CompileStatic
class RatedMovie extends Movie {

  Double rating
}
