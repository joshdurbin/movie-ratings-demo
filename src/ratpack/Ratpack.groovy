import io.durbs.movieratings.MovieRatingsModule
import io.durbs.movieratings.handler.CreateAccountHandler
import io.durbs.movieratings.handler.LoginHandler
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

    post('register', CreateAccountHandler)
    post('login', LoginHandler)
  }
}
