import io.durbs.movieratings.MovieRatingsModule
import io.durbs.movieratings.handler.auth.JWTTokenHandler
import io.durbs.movieratings.handler.auth.RegistrationHandler
import io.durbs.movieratings.handler.auth.LoginHandler
import ratpack.config.ConfigData
import ratpack.handling.Chain
import ratpack.jackson.Jackson
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
    prefix('api') { Chain chain ->

      chain.all(JWTTokenHandler)

      get('things') {
        render Jackson.json('tada')
      }
    }
  }
}
