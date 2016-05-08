package io.durbs.movieratings.model.persistent

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.bson.types.ObjectId

@Canonical
@CompileStatic
class User {

  @JsonIgnore
  ObjectId id

  String username
  String name
  String emailAddress
  String password
}
