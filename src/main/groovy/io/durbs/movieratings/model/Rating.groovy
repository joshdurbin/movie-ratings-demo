package io.durbs.movieratings.model

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.bson.types.ObjectId

@Canonical
@CompileStatic
class Rating {

  ObjectId id

  ObjectId userId
  ObjectId movieId
  Integer rating
  String comment
}
