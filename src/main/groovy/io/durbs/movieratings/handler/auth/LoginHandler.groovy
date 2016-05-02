package io.durbs.movieratings.handler.auth

import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.util.logging.Slf4j
import io.durbs.movieratings.Constants
import io.durbs.movieratings.model.User
import io.durbs.movieratings.services.AuthenticationService
import ratpack.groovy.handling.GroovyContext
import ratpack.groovy.handling.GroovyHandler
import ratpack.jackson.Jackson
import rx.functions.Func1

import static ratpack.jackson.Jackson.fromJson

@Singleton
@Slf4j
class LoginHandler extends GroovyHandler {

  @Inject
  AuthenticationService authenticationService

  @Override
  protected void handle(GroovyContext context) {

    context.parse(fromJson(User))
      .observe()
      .flatMap({ final User user ->

      authenticationService.authenticate(user.username, user.password)
    } as Func1)
      .doOnError { final Throwable throwable ->

      context.clientError(500)
    }.defaultIfEmpty(Constants.PLACE_HOLDER_INVALID_JWT_TOKEN)
      .subscribe { final String jwt ->

      if (jwt != Constants.PLACE_HOLDER_INVALID_JWT_TOKEN) {

        context.render(Jackson.json(jwt))
      } else {

        context.render(Jackson.json(jwt))
      }
    }
  }
}
