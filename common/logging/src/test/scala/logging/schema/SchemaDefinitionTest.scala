package logging.schema

import akka.actor.ActorSystem
import io.circe.Json
import io.circe.parser.parse
import logging.filter.ServiceFilter
import logging.model.levels.Levels
import logging.service.LoggingService
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros._
import sangria.marshalling.circe._
import sangria.schema.Schema
import scribe.Level._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SchemaDefinitionTest extends AnyWordSpec with Matchers {
  implicit val system: ActorSystem = ActorSystem("test")

  val servicePackage                       = "service.test"
  val schema: Schema[LoggingService, Unit] = SchemaDefinition.schema
  val loggingService: LoggingService =
    LoggingService(servicePackage, Levels(service = Warn, libraries = Debug))

  "SchemaDefinition" when {
    "receives levels query" should {
      "return current logging levels" in {
        val levelsQuery =
          graphql"""
            query {
              levels {
                service
                libraries
              }
            }
          """

        val queryFuture = query(levelsQuery).map(parseData(_, "levels"))
        val result      = Await.result(queryFuture, 10.seconds)

        val expectedLevels = loggingService.currentLevels
        result should be(levelsJson(expectedLevels))
      }
    }

    "receives changeLevels mutation" should {
      "change both levels" in {
        val levels = Levels(service = Trace, libraries = Error)
        val mutation =
          graphql"""
            mutation($$levelsChangeInput: LevelsChangeInput!) {
              changeLevels(levelsChangeInput: $$levelsChangeInput) {
                service
                libraries
              }
            }
          """
        val variables = levelsVariables(levels)

        val queryFuture =
          query(mutation, variables).map(parseData(_, "changeLevels"))
        val result = Await.result(queryFuture, 10.seconds)

        result should be(levelsJson(levels))
        checkServiceFilter(levels)
      }

      "change only service level" in {
        // TODO: Add test
      }

      "change only libraries level" in {
        // TODO: Add test
      }
    }
  }

  // TODO: Change methods order

  private def query(document: Document, variables: Json = Json.obj()) =
    Executor.execute(
      schema = schema,
      queryAst = document,
      variables = variables,
      userContext = loggingService
    )

  private def levelsVariables(levels: Levels) =
    Json.obj("levelsChangeInput" -> levelsJson(levels))

  private def levelsJson(levels: Levels) =
    json(s"""|{
             |  "service": "${levels.service.name}",
             |  "libraries": "${levels.libraries.name}"
             |}
             |""".stripMargin)

  private def json(string: String) =
    parse(string) match {
      case Right(variables) => variables
      case Left(failure)    => throw failure
    }

  private def parseData(json: Json, field: String) =
    json.hcursor.downField("data").get[Json](field) match {
      case Right(value)  => value
      case Left(failure) => sys.error(failure.message)
    }

  private def checkServiceFilter(levels: Levels) = {
    val serviceFilter =
      scribe.Logger.root.modifiers.find(_.id == ServiceFilter.id) match {
        case Some(filter: ServiceFilter) => filter
        case _                           => fail("ServiceFilter not found on scribe logger")
      }
    serviceFilter.servicePackage should be(servicePackage)
    serviceFilter.levels should be(levels)
  }
}
