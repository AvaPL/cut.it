package link.store.service

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.data.EitherT
import cats.implicits._
import io.circe.Error
import io.circe.generic.auto._
import io.circe.parser.decode
import link.store.elasticsearch.ElasticConnector
import link.store.flow.LinkRetrievedMessageFlow
import links.elasticsearch.Index
import links.model.Link

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

case class LinkRetrievalService(
    elasticConnector: ElasticConnector,
    linkRetrievedMessageFlow: LinkRetrievedMessageFlow
)(implicit
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
    link.onComplete(sendLinkRetrievedMessage(id))
    EitherT(link).rethrowT.map(_.uri)
  }

  private def sendLinkRetrievedMessage(
      id: String
  )(decodingResult: Try[Either[Error, Link]]): Unit =
    decodingResult.foreach {
      case Right(link) =>
        linkRetrievedMessageFlow.sendLinkRetrievedMessage(link)
      case Left(error) =>
        scribe.warn(
          s"There is a malformed document in index ${Index.linkStoreIndex} with id $id that caused a decoding error: $error"
        )
    }
}
