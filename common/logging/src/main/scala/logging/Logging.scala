package logging

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import io.circe.generic.auto._
import scribe.Level
import scribe.Level.{Debug, Error, Info, Trace, Warn}

trait Logging {
  this: App =>

  private implicit val system: ActorSystem = ActorSystem("logging")

  // Additional final modifier is used because of scope leakage:
  // https://github.com/scala/bug/issues/11339
  private[logging] final val loggingRoute = path("logging") {
    post {
      entity(as[ScribeSettings]) { settings =>
        setLoggingSettings(settings)
      }
    }
  }

  setMinimumLevel(defaultMinimumLoggingLevel)
  if (enableLoggingServer) {
    Http().newServerAt("0.0.0.0", 1065).bind(loggingRoute)
    scribe.info("Logging server started")
  }

  private def setLoggingSettings(settings: ScribeSettings) = {
    val minimumLevel = parseMinimumLevel(settings.minimumLevel)
    if (minimumLevel.isDefined) {
      setMinimumLevel(minimumLevel.get)
      complete(StatusCodes.NoContent)
    } else
      complete(
        StatusCodes.BadRequest,
        minimumLevelErrorJson(settings.minimumLevel)
      )
  }

  private def parseMinimumLevel(minimumLevel: String) =
    minimumLevel.toUpperCase match {
      case Trace.name => Some(Trace)
      case Debug.name => Some(Debug)
      case Info.name  => Some(Info)
      case Warn.name  => Some(Warn)
      case Error.name => Some(Error)
      case _          => None
    }

  private def setMinimumLevel(minimumLevel: Level): Unit = {
    scribe.Logger.root
      .clearHandlers()
      .withHandler(minimumLevel = Some(minimumLevel))
      .replace()
    scribe.info(s"Minimum logging level set to: ${minimumLevel.name}")
  }

  private def minimumLevelErrorJson(invalidLevel: String) =
    Json.obj(
      (
        "error",
        Json.fromString(
          s"minimumLevel must be in [${List(Trace, Debug, Info, Warn, Error)
            .map(_.name)
            .mkString(", ")}] but was '$invalidLevel'"
        )
      )
    )

  def defaultMinimumLoggingLevel: Level = Info

  def enableLoggingServer: Boolean = true
}
