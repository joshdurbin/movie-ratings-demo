package io.durbs.movieratings.model.persistent

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@Canonical
@CompileStatic
class RatedMovie extends Movie {

  Double rating
}
