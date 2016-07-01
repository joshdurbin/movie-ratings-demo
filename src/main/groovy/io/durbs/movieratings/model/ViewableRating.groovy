package io.durbs.movieratings.model

import groovy.transform.Canonical
import groovy.transform.CompileStatic

@Canonical
@CompileStatic
class ViewableRating extends Rating {

  String username
  String usersGivenName
}