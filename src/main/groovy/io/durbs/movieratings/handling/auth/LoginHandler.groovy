package io.durbs.movieratings.handling.auth

import com.google.common.net.HttpHeaders
import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.transform.CompileStatic
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
@CompileStatic
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

        context.response.headers.add(HttpHeaders.AUTHORIZATION, "${Constants.BEARER_AUTHORIZATION_SCHEMA_KEY} ${jwt}")
        context.render(Jackson.json(jwt))
      } else {

        context.clientError(401)
      }
    }
  }
}
