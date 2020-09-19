package http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import config.{Config, ServerConfig}
import http.route.{GraphQl, GraphiQl}
import pureconfig.generic.auto._
import repository.UserRepository
import scribe.Level

import scala.concurrent.ExecutionContextExecutor

object Server extends App with Config[ServerConfig] {
  implicit val system: ActorSystem                        = ActorSystem("graphql")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  scribe.Logger.root
    .clearHandlers()
    .withHandler(minimumLevel = Some(Level.Debug))
    .replace()

  val userRepository = UserRepository()
  val route          = GraphQl(userRepository).route ~ GraphiQl.route

  override def defaultConfig = ServerConfig(port = 8080)

  Http().newServerAt("localhost", config.port).bind(route)
  scribe.info("GraphQL server started")

