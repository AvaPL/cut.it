package link.store.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

case class LinkRetrievalService()(implicit as: ActorSystem) {
  val route: Route = path(Segment) { id =>
    get {
      complete(
        StatusCodes.MovedPermanently,
        Seq(Location("http://google.com")), // TODO: Retrieve link with given id
        ""
      )
    }
  }
}
