package io.durbs.movieratings.model

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@Canonical
@CompileStatic
class ViewableRating {

  String username
  String usersGivenName
  Integer rating
  String comment
}
