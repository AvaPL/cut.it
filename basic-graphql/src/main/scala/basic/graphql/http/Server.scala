package basic.graphql.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import basic.graphql.config.ServerConfig
import basic.graphql.http.route.{GraphQl, GraphiQl}
import basic.graphql.repository.UserRepository
import config.Config
import logging.Logging
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContextExecutor

object Server extends App with Config[ServerConfig] with Logging {
  implicit val system: ActorSystem                        = ActorSystem("graphql")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val userRepository = UserRepository()
  val route          = GraphQl(userRepository).route ~ GraphiQl.route

  Http().newServerAt("0.0.0.0", config.port).bind(route)
  scribe.info(s"GraphQL server started at port ${config.port}")

  override def defaultConfig = ServerConfig(port = 8080)
}
