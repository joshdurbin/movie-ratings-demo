package io.durbs.movieratings.handling.chainaction

import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.util.logging.Slf4j
import io.durbs.movieratings.Constants
import io.durbs.movieratings.model.Movie
import io.durbs.movieratings.model.User
import io.durbs.movieratings.services.AuthenticationService
import ratpack.groovy.handling.GroovyChainAction
import ratpack.jackson.Jackson
import rx.functions.Func1

import javax.validation.ConstraintViolation
import javax.validation.Validator

import static ratpack.jackson.Jackson.fromJson

@Singleton
@Slf4j
class AuthEndpoint extends GroovyChainAction {

  @Inject
  AuthenticationService authenticationService

  @Inject
  Validator validator

  @Override
  void execute() throws Exception {

    post('register') {

      context.parse(fromJson(User))
        .observe()
        .flatMap({ final User user ->

        final Set<ConstraintViolation<User>> violations = validator.validate(user)
        if (!violations.empty) {
          throw new IllegalArgumentException("There are a number of constraint violations while creating the user ${violations}")
        }

        authenticationService.createAccount(user)
      } as Func1)
        .subscribe { final String jwt ->

        context.render(Jackson.json(jwt))
      }
    }

    post('login') {

      parse(fromJson(User))
        .observe()
        .flatMap({ final User user ->

        authenticationService.authenticate(user.username, user.password)
      } as Func1)
        .doOnError { final Throwable throwable ->

        clientError(500)
      }.defaultIfEmpty(Constants.PLACE_HOLDER_INVALID_JWT_TOKEN)
        .subscribe { final String jwt ->

        if (jwt != Constants.PLACE_HOLDER_INVALID_JWT_TOKEN) {

          render(Jackson.json(jwt))
        } else {

          clientError(401)
        }
      }
    }
  }
}
