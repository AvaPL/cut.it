package link.store.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.data.EitherT
import cats.implicits._
import io.circe.generic.auto._
import io.circe.parser.decode
import link.store.elasticsearch.ElasticConnector
import links.elasticsearch.Index
import links.model.Link

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

case class LinkRetrievalService(elasticConnector: ElasticConnector)(implicit
    as: ActorSystem
) {
  val route: Route = path(Segment) { id =>
    get {
      onComplete(retrieveLinkUri(id)) {
        case Success(uri) =>
          scribe.trace(s"Redirecting user from /$id to $uri")
          complete(StatusCodes.MovedPermanently, Seq(Location(uri)), "")
        case Failure(exception) =>
          scribe.trace(s"Link with id $id not found")
          // TODO: Prettify 404 page
          complete(StatusCodes.NotFound, exception.getMessage)
      }
    }
  }

  private def retrieveLinkUri(id: String)(implicit as: ActorSystem) = {
    implicit val ec: ExecutionContext = as.dispatcher
    val document                      = elasticConnector.getDocument(Index.linkStoreIndex, id)
    val link                          = document.map(decode[Link])
    EitherT(link).rethrowT.map(_.uri)
  }
}
