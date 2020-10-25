package logging.http.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import logging.schema.SchemaDefinition
import logging.service.LoggingService
import sangria.ast.Document
import sangria.execution.Executor
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.validation.{QueryValidator, Violation}

import scala.concurrent.ExecutionContext

// TODO: Move common parts to graphql module
case class GraphQl(loggingService: LoggingService)(implicit
    ec: ExecutionContext
) {
  val route: Route = path("graphql") {
    post {
      entity(as[Json]) { request =>
        graphqlEndpoint(request)
      }
    }
  }

  private def graphqlEndpoint(request: Json) =
    extractQuery(request) match {
      case Right((queryAst, operation, variables)) =>
        validateAndExecuteQuery(queryAst, operation, variables)
      case Left(exception) => badRequest(exception)
    }

  private def extractQuery(request: Json) = {
    val query =
      extractString(request, "query").flatMap(QueryParser.parse(_).toEither)
    val operation = extractString(request, "operationName").toOption
    val variables = extractVariables(request)
    query.map((_, operation, variables))
  }

  private def extractString(request: Json, field: String) =
    request.hcursor.get[String](field)

  private def extractVariables(request: Json) =
    request.hcursor
      .downField("variables")
      .focus
      .filter(_.toString != "null")
      .getOrElse(Json.obj())

  private def validateAndExecuteQuery(
      queryAst: Document,
      operation: Option[String],
      variables: Json
  ) =
    validateQuery(queryAst) match {
      case violations if violations.isEmpty =>
        complete(executeQuery(queryAst, operation, variables))
      case violations => badRequest(violations)
    }

  private def validateQuery(queryAst: Document) =
    QueryValidator.default.validateQuery(
      SchemaDefinition.schema,
      queryAst
    )

  private def executeQuery(
      queryAst: Document,
      operation: Option[String],
      variables: Json
  ) = {
    scribe.debug(
      s"""Executing query
         |Query: 
         |${queryAst.source.getOrElse("")}
         |Operation: ${operation.getOrElse("")}
         |Variables: 
         |$variables
         |""".stripMargin
    )
    Executor.execute(
      SchemaDefinition.schema,
      queryAst,
      loggingService,
      variables = variables,
      operationName = operation
    )
  }

  private def badRequest(violations: Vector[Violation]) = {
    scribe.debug(s"""Bad request
                    |Violations: 
                    |${violations.map(_.errorMessage).mkString("\n")}
                    |""".stripMargin)
    complete(
      StatusCodes.BadRequest,
      Json.obj(
        (
          "errors",
          Json.obj(
            ("message", Json.fromString("Query does not pass validation.")),
            (
              "violations",
              Json.arr(violations.map(v => Json.fromString(v.errorMessage)): _*)
            )
          )
        )
      )
    )
  }

  private def badRequest(throwable: Throwable) = {
    scribe.debug("Bad request", throwable)
    complete(
      StatusCodes.BadRequest,
      Json.obj(
        (
          "errors",
          Json.obj(
            ("message", Json.fromString(throwable.getMessage))
          )
        )
      )
    )
  }
}
