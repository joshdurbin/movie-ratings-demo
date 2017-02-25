package io.durbs.movieratings.handling.handler

import com.google.inject.Inject
import com.google.inject.Singleton
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.durbs.movieratings.services.MovieService
import org.bson.types.ObjectId
import ratpack.groovy.handling.GroovyContext
import ratpack.groovy.handling.GroovyHandler
import ratpack.registry.Registry

@Singleton
@Slf4j
@CompileStatic
class MovieIDExtractionAndVerificationHandler extends GroovyHandler {

  @Inject
  MovieService movieService

  @Override
  protected void handle(GroovyContext context) {

    String movieId = context.pathTokens.get('id')

    if (ObjectId.isValid(movieId)) {

      ObjectId movieObjectId = new ObjectId(movieId)

      movieService
        .movieExists(movieObjectId)
        .single()
        .subscribe({ Boolean movieExists ->

        if (movieExists) {

          context.next(Registry.single(movieObjectId))

        } else {
          context.clientError(404)
        }
      })

    } else {

      context.clientError(404)
    }
  }
}