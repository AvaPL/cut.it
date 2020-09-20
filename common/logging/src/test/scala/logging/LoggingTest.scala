package logging

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scribe.Level
import scribe.Level._

class LoggingTest
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with ScalatestRouteTest {

  var logging: Logging = new {} with App with Logging {
    override def enableLoggingServer: Boolean      = false
    override def defaultMinimumLoggingLevel: Level = Error
  }

  "Logging" when {

    "started" should {
      s"include ${logging.defaultMinimumLoggingLevel} logging level" in {
        scribeLevelShouldBe(logging.defaultMinimumLoggingLevel)
      }
    }

    "sending POST to /logging" should {
      val levels = List(Trace, Debug, Info, Warn, Error)

      "change logging level for each possible level" in {
        for (level <- levels)
          Post(
            "/logging",
            minimumLevelEntity(level.name)
          ) ~> logging.route ~> check {
            status should be(StatusCodes.NoContent)
            scribeLevelShouldBe(level)
          }
      }

      "change logging level ignoring letter case" in {
        for (level <- levels)
          Post(
            "/logging",
            minimumLevelEntity(level.name.toLowerCase.capitalize)
          ) ~> logging.route ~> check {
            status should be(StatusCodes.NoContent)
            scribeLevelShouldBe(level)
          }
      }

      "ignore invalid logging level" in {
        Post(
          "/logging",
          minimumLevelEntity("invalid")
        ) ~> logging.route ~> check {
          status should be(StatusCodes.BadRequest)
          scribeLevelShouldBe(logging.defaultMinimumLoggingLevel)
        }
      }
    }
  }

  override protected def afterEach(): Unit =
    try super.afterEach()
    finally logging = new {} with App with Logging {
      override def enableLoggingServer: Boolean = false
    }

  private def scribeLevelShouldBe(level: Level) = {
    scribe.Logger.root.includes(level) should be(true)
    scribe.Logger.root.includes(
      Level(
        "excluded",
        // 1 point difference is enough, predefined levels values interval is 100
        level.value - 1
      )
    ) should be(false)
  }

  private def minimumLevelEntity(minimumLevel: String) = {
    val json = s"""|{
                   |  "minimumLevel": "$minimumLevel"
                   |}
                   |""".stripMargin
    HttpEntity(ContentTypes.`application/json`, json)
  }
}
