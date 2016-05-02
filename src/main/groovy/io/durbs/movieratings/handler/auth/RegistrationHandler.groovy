package io.durbs.movieratings.handler.auth

import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.util.logging.Slf4j
import io.durbs.movieratings.model.User
import io.durbs.movieratings.services.AuthenticationService
import ratpack.groovy.handling.GroovyContext
import ratpack.groovy.handling.GroovyHandler
import ratpack.jackson.Jackson
import rx.functions.Func1

import static ratpack.jackson.Jackson.fromJson

@Singleton
@Slf4j
class RegistrationHandler extends GroovyHandler {

  @Inject
  AuthenticationService authenticationService

  @Override
  protected void handle(GroovyContext context) {

    context.parse(fromJson(User))
      .observe()
      .flatMap({ final User user ->

      authenticationService.createAccount(user.username, user.password, user.emailAddress)
    } as Func1)
    .subscribe { final String jwt ->

      context.render(Jackson.json(jwt))
    }
  }
}
