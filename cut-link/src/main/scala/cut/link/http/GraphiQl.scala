package cut.link.http

import akka.http.scaladsl.server.Directives.{get, getFromResource, path}
import akka.http.scaladsl.server.Route

object GraphiQl extends RouteDefinition {
  override val route: Route = path("graphiql") {
    get {
      getFromResource("graphiql.html")
    }
  }
}
