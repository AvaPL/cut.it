package logging.http.route

import akka.http.scaladsl.server.Directives.{get, getFromResource, path}
import akka.http.scaladsl.server.Route

// TODO: Move common parts to links module
object GraphiQl {
  val route: Route = path("graphiql") {
    get {
      getFromResource("graphiql.html")
    }
  }
}
