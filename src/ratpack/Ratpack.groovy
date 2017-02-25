import io.durbs.movieratings.MovieRatingsConfig
import io.durbs.movieratings.MovieRatingsModule
import io.durbs.movieratings.handling.chainaction.AuthEndpoint
import io.durbs.movieratings.handling.chainaction.MovieRestEndpoint
import ratpack.hystrix.HystrixModule
import ratpack.rx.RxRatpack

import static ratpack.groovy.Groovy.ratpack

ratpack {

  serverConfig {
    yaml('application.yaml')
    sysProps('ratingsdemo.')
    require("", MovieRatingsConfig)
  }

  bindings {

    RxRatpack.initialize()

    module MovieRatingsModule
    module HystrixModule.newInstance()
  }

  handlers {

    all {

      // cross origin resource sharing header
      response.headers.add('Access-Control-Allow-Methods', 'GET, POST, DELETE')
      response.headers.add('Access-Control-Allow-Origin', '*')
      response.headers.add('Access-Control-Allow-Headers', 'Content-Type')

      next()
    }

    prefix('api/auth', AuthEndpoint)
    prefix('api', MovieRestEndpoint)
  }
}
