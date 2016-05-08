package io.durbs.movieratings.model.persistent

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.bson.types.ObjectId

@Canonical
@CompileStatic
class Movie {

  ObjectId id

  String name
  String description
  String imageURI
}
