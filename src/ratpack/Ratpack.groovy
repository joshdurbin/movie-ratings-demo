import io.durbs.movieratings.MovieRatingsModule
import io.durbs.movieratings.handling.chainaction.MovieRestEndpoint
import io.durbs.movieratings.handling.handler.RegistrationHandler
import io.durbs.movieratings.handling.handler.LoginHandler
import ratpack.config.ConfigData
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
  }

  handlers {

    post('register', RegistrationHandler)
    post('login', LoginHandler)
    prefix('api', MovieRestEndpoint)
  }
}
