package http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import sangria.parser.QueryParser
import io.circe.syntax._
import sangria.ast.Document

import scala.util.{Failure, Success}

object Server extends App {
  implicit val system: ActorSystem = ActorSystem("graphql")

  val route = path("graphql") {
    post {
      entity(as[Json]) { request => graphqlEnpoint(request) }
    }
  }

  def graphqlEnpoint(request: Json) = {
    val query = request.hcursor
      .downField("query")
      .as[String] match { // TODO: Cleanup code that uses hcursor
      case Right(query) => query
      // TODO: Handle Left
    }
    QueryParser.parse(query) match {
      case Success(queryAst) =>
        val operation = request.hcursor.get[String]("operationName").toOption
        val variables =
          request.hcursor.downField("variables").focus.getOrElse(Json.obj())
        complete(executeQuery(queryAst, operation, variables))
      case Failure(exception) =>
        complete(
          StatusCodes.BadRequest,
          ("errors" -> List("message" -> exception.getMessage)).asJson
        )
    }
  }

  def executeQuery(
      queryAst: Document,
      operation: Option[String],
      variables: Json
  ) = ???
}
