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

/**
  * Trait that allows a class to log messages using scribe. It also starts
  * a config server at 0.0.0.0:1065 that allows dynamical logging level
  * change. To change logging level use POST on /logging endpoint.
  *
  * Supported logging levels are:
  *  - trace
  *  - debug
  *  - info
  *  - warn
  *  - error
  *
  * @example {{{
  * POST /logging
  * {
  *   "minimumLevel": "warn"
  * }
  * }}}
  */
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

  setMinimumLoggingLevel(defaultMinimumLoggingLevel)
  if (enableLoggingServer) {
    Http().newServerAt("0.0.0.0", 1065).bind(loggingRoute)
    scribe.info("Logging server started")
  }

  def defaultMinimumLoggingLevel: Level = Info

  /**
    * Determines if the logging server should be started, defaults to `true`. It
    * is only invoked on init so it should be overridden in derived classes if
    * other behavior is desired.
    */
  def enableLoggingServer: Boolean = true

  private def setLoggingSettings(settings: ScribeSettings) = {
    val minimumLevel = parseMinimumLevel(settings.minimumLevel)
    if (minimumLevel.isDefined) {
      setMinimumLoggingLevel(minimumLevel.get)
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

  def setMinimumLoggingLevel(minimumLevel: Level): Unit = {
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
}
