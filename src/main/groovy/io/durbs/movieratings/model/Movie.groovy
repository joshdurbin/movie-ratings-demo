package io.durbs.movieratings.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.bson.types.ObjectId

@Canonical
@CompileStatic
class Movie {

  ObjectId id

  String name
  String imdbId
  String description
  String posterImageURI
  Integer yearReleased
  List<String> genre
  List<String> actors
  List<String> directors
  List<String> writers
  List<String> languages
  String mpaaRating
  String awards

  @Delegate
  @JsonIgnore
  ComputedUserRating computedRating

  @Delegate
  @JsonIgnore
  ExternalRating externalRating
}
