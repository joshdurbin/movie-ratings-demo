package io.durbs.movieratings.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.bson.types.ObjectId

import javax.validation.constraints.Max
import javax.validation.constraints.Min

@Canonical
@CompileStatic
class Rating {

  @JsonIgnore
  ObjectId id

  @JsonIgnore
  ObjectId userId

  @JsonIgnore
  ObjectId movieId

  @Min(value = 1L, message = 'The minimum vote is 1')
  @Max(value = 10L, message = 'The maximum vote is 10')
  Integer rating
}
