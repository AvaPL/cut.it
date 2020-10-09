package graphql.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import config.Config
import graphql.config.{KafkaConfig, ServerConfig}
import graphql.http.route.{GraphQl, GraphiQl}
import graphql.service.LinkService
import logging.Logging
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContextExecutor

object Server extends App with Config[ServerConfig] with Logging {
  implicit val system: ActorSystem                        = ActorSystem("cut-link")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val linkService = LinkService(config.kafka.kafkaProducer)
  val route       = GraphQl(linkService).route ~ GraphiQl.route

  Http().newServerAt("0.0.0.0", config.port).bind(route)
  scribe.info(s"cut-link server started at port ${config.port}")

  override def defaultConfig =
    ServerConfig(port = 8080, KafkaConfig(bootstrapServers = ""))
}
