package cut.link.http

import akka.http.scaladsl.server.Route

trait RouteDefinition {
  def route: Route
}
