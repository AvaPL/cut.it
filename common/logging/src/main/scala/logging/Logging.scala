package logging

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import graphql.{GraphQl, GraphiQl}
import logging.model.levels.Levels
import logging.schema.SchemaDefinition
import logging.service.LoggingService
import scribe.Level.{Info, Warn}

import scala.concurrent.ExecutionContextExecutor

/**
  * Trait that allows a class to log messages using scribe. It also starts
  * a config server at 0.0.0.0:1065 that allows dynamical logging level
  * change. To change logging level use GraphiQL interface on /graphiql
  * endpoint.
  *
  * Supported logging levels are:
  *  - trace
  *  - debug
  *  - info
  *  - warn
  *  - error
  *
  * @example {{{
  * query {
  *   levels {
  *     service
  *     libraries
  *   }
  * }
  * }}}
  *
  * @example {{{
  * mutation {
  *   changeLevels(levelsChangeInput: {service: "info", libraries: "warn"}) {
  *     service
  *     libraries
  *   }
  * }
  * }}}
  */
trait Logging {
  this: App =>

  private implicit val system: ActorSystem = ActorSystem("logging")
  private implicit val executionContext: ExecutionContextExecutor =
    system.dispatcher

  private val loggingService =
    LoggingService(servicePackage, defaultLoggingLevels)

  /**
    * The root package for which `service` level will be used.
    */
  def servicePackage: String

  /**
    * Default logging level for the service package and for libraries outside
    * the service package.
    */
  def defaultLoggingLevels: Levels = Levels(service = Info, libraries = Warn)

  loggingService.changeLevels(defaultLoggingLevels)
  if (enableLoggingServer) {
    Http().newServerAt("0.0.0.0", 1065).bind(route)
    scribe.info("Logging server started")
  }

  /**
    * Determines if the logging server should be started, defaults to `true`.
    * It is only invoked on init so it should be overridden in derived classes
    * if other behavior is desired.
    */
  def enableLoggingServer: Boolean = true

  private def route =
    GraphQl(SchemaDefinition.schema, loggingService).route ~ GraphiQl.route
}
