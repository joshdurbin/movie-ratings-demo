package io.durbs.movieratings.model

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.bson.types.ObjectId
import org.hibernate.validator.constraints.Email
import org.hibernate.validator.constraints.NotEmpty

@Canonical
@CompileStatic
class User {

  @JsonIgnore
  ObjectId id

  @NotEmpty(message = 'A username is required.')
  String username

  @NotEmpty(message = 'A full name is required.')
  String name

  @Email(message = 'An email address is required.')
  String emailAddress

  @NotEmpty(message = 'Obviously a password is required!')
  String password
}
