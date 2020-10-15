package cut.link.http

import akka.http.scaladsl.server.Directives.{get, getFromResource, path}
import akka.http.scaladsl.server.Route

object GraphiQl {
  val route: Route = path("graphiql") {
    get {
      getFromResource("graphiql.html")
    }
  }
}
