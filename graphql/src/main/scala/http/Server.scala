package http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import io.circe.syntax._
import repository.UserRepository
import sangria.ast.Document
import sangria.execution.Executor
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import schema.SchemaDefinition

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object Server extends App {
  implicit val system: ActorSystem                        = ActorSystem("graphql")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val userRepository = UserRepository()

  val route = path("graphql") {
    post {
      entity(as[Json]) { request => graphqlEndpoint(request) }
    }
  } ~ path("graphiql") {
    get {
      getFromResource("graphiql.html")
    }
  }

  Http().newServerAt("localhost", 8080).bind(route)

  def graphqlEndpoint(request: Json) = {
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
          request.hcursor
            .downField("variables")
            .focus
            .filter(_.toString != "null")
            .getOrElse(Json.obj())
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
  ) = {
    Executor.execute(
      SchemaDefinition.schema,
      queryAst,
      userRepository,
      variables = variables,
      operationName = operation
    )
  }
}
