import io.durbs.movieratings.MovieRatingsModule
import io.durbs.movieratings.handling.chainaction.AuthEndpoint
import io.durbs.movieratings.handling.chainaction.MovieRestEndpoint
import ratpack.config.ConfigData
import ratpack.hystrix.HystrixModule
import ratpack.rx.RxRatpack

import static ratpack.groovy.Groovy.ratpack

ratpack {

  bindings {

    RxRatpack.initialize()

    bindInstance(ConfigData, ConfigData.of { c ->
      c.yaml("$serverConfig.baseDir.file/application.yaml")
      c.env()
      c.sysProps()
    })

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
