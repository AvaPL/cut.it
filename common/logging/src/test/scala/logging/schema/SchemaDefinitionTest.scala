package logging.schema

import akka.actor.ActorSystem
import io.circe.Json
import io.circe.parser.parse
import logging.filter.ServiceFilter
import logging.model.levels.{Levels, LevelsChange}
import logging.service.LoggingService
import org.scalatest.BeforeAndAfterEach
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

class SchemaDefinitionTest
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach {
  implicit val system: ActorSystem = ActorSystem("test")

  val servicePackage                       = "service.test"
  val schema: Schema[LoggingService, Unit] = SchemaDefinition.schema
  val initialLevels: Levels                = Levels(service = Info, libraries = Info)
  val loggingService: LoggingService =
    LoggingService(servicePackage, initialLevels)

  override protected def beforeEach(): Unit =
    loggingService.changeLevels(initialLevels)

  "SchemaDefinition" when {
    "receives levels query" should {
      "return current logging levels" in {
        val queryFuture = query(levelsQuery).map(parseData(_, "levels"))
        val result      = Await.result(queryFuture, 10.seconds)

        val expectedLevels = loggingService.currentLevels
        result should be(levelsJson(expectedLevels))
      }
    }

    "receives changeLevels mutation" should {
      "change both levels" in {
        val targetLevels = Levels(service = Trace, libraries = Error)
        val variables    = levelsChangeInput(levelsJson(targetLevels))

        changeLevelsAndCheck(variables, targetLevels)
      }

      "change only service level" in {
        val levelsChange   = LevelsChange(service = Some(Debug))
        val variables      = levelsChangeInput(levelsChangeJson(levelsChange))
        val expectedLevels = loggingService.currentLevels.copy(service = Debug)

        changeLevelsAndCheck(variables, expectedLevels)
      }

      "change only libraries level" in {
        val levelsChange   = LevelsChange(libraries = Some(Warn))
        val variables      = levelsChangeInput(levelsChangeJson(levelsChange))
        val expectedLevels = loggingService.currentLevels.copy(libraries = Warn)

        changeLevelsAndCheck(variables, expectedLevels)
      }
    }
  }

  private def query(document: Document, variables: Json = Json.obj()) =
    Executor.execute(
      schema = schema,
      queryAst = document,
      variables = variables,
      userContext = loggingService
    )

  private def levelsQuery =
    graphql"""
            query {
              levels {
                service
                libraries
              }
            }
          """

  private def parseData(json: Json, field: String) =
    json.hcursor.downField("data").get[Json](field) match {
      case Right(value)  => value
      case Left(failure) => sys.error(failure.message)
    }

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

  private def levelsChangeInput(json: Json) =
    Json.obj("levelsChangeInput" -> json)

  private def changeLevelsAndCheck(variables: Json, expectedLevels: Levels) = {
    val queryFuture = query(changeLevelsMutation, variables).map(
      parseData(_, "changeLevels")
    )
    val result = Await.result(queryFuture, 10.seconds)

    result should be(levelsJson(expectedLevels))
    checkServiceFilter(expectedLevels)
  }

  private def changeLevelsMutation =
    graphql"""
            mutation($$levelsChangeInput: LevelsChangeInput!) {
              changeLevels(levelsChangeInput: $$levelsChangeInput) {
                service
                libraries
              }
            }
          """

  private def checkServiceFilter(levels: Levels) = {
    val serviceFilter =
      scribe.Logger.root.modifiers.find(_.id == ServiceFilter.id) match {
        case Some(filter: ServiceFilter) => filter
        case _                           => fail("ServiceFilter not found on scribe logger")
      }
    serviceFilter.servicePackage should be(servicePackage)
    serviceFilter.levels should be(levels)
  }

  private def levelsChangeJson(levelsChange: LevelsChange) =
    json(s"""|{
             |  ${levelsChange.service.map(service => s""""service": "${service.name}"""").getOrElse("")}
             |  ${levelsChange.libraries.map(libraries => s""""libraries": "${libraries.name}"""").getOrElse("")}
             |}
             |""".stripMargin)
}
