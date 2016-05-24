package io.durbs.movieratings.model

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.bson.types.ObjectId
import org.hibernate.validator.constraints.NotEmpty
import org.hibernate.validator.constraints.URL

@Canonical
@CompileStatic
class Movie {

  ObjectId id

  @NotEmpty(message = 'Movie name is required.')
  String name

  @NotEmpty(message = 'Movie IMDB ID is required.')
  String imdbId

  @NotEmpty(message = 'Movie description is required.')
  String description

  @URL(message = 'Movie post image URI is required.')
  String posterImageURI

  @NotEmpty(message = 'Movie year released is required.')
  Integer yearReleased

  ComputedUserRating computedRating
  ExternalRating externalRating
}
