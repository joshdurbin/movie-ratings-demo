package io.durbs.movieratings.handling.handler

import com.google.common.net.HttpHeaders
import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.Constants
import io.durbs.movieratings.model.User
import io.durbs.movieratings.services.AuthenticationService
import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.groovy.handling.GroovyContext
import ratpack.groovy.handling.GroovyHandler
import ratpack.registry.Registry

@Singleton
@Slf4j
@CompileStatic
class JWTTokenHandler extends GroovyHandler {

  @Inject
  AuthenticationService authenticationService

  @Override
  protected void handle(GroovyContext context) {

    if (context.request.headers.contains(HttpHeaders.AUTHORIZATION)
      && context.request.headers.get(HttpHeaders.AUTHORIZATION)?.startsWith(Constants.BEARER_AUTHORIZATION_SCHEMA_KEY)) {

      final String token = context.request.headers.get(HttpHeaders.AUTHORIZATION) - Constants.BEARER_AUTHORIZATION_PREFIX

      authenticationService.validateToken(token)
        .doOnError { final Throwable throwable ->

        log.error('An error occurred attempting to validate the JWT token', throwable)
        context.clientError(HttpResponseStatus.UNAUTHORIZED.code())

      }.subscribe { final User user ->

        context.next(Registry.single(user))
      }
    } else {

      context.clientError(HttpResponseStatus.UNAUTHORIZED.code())
    }
  }
}
