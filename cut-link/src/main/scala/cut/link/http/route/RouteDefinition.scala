package cut.link.http.route

import akka.http.scaladsl.server.Route

trait RouteDefinition {
  def route: Route
}
