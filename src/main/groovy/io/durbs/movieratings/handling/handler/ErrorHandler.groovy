package io.durbs.movieratings.handling.handler

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.StackTraceUtils
import ratpack.error.ServerErrorHandler
import ratpack.handling.Context
import ratpack.jackson.Jackson

@CompileStatic
@Slf4j
class ErrorHandler implements ServerErrorHandler {

  @Override
  void error(Context context, Throwable throwable) throws Exception {

    log.warn("Something blew up", throwable)

    context.with {

      render Jackson.json(new Error(uri: context.request.rawUri,
        exception: StackTraceUtils.deepSanitize(throwable)))
      context.clientError(403)
    }
  }

  @Immutable
  private static class Error {

    String uri
    String exception
  }
}
