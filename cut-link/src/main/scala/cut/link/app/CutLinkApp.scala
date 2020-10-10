package cut.link.app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import config.Config
import cut.link.config.{KafkaConfig, CutLinkConfig}
import cut.link.http.{GraphQl, GraphiQl}
import cut.link.service.LinkService
import logging.Logging
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContextExecutor

object CutLinkApp extends App with Config[CutLinkConfig] with Logging {
  implicit val system: ActorSystem                        = ActorSystem("cut-link")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val linkService = LinkService(config.kafka.kafkaProducer)
  val route       = GraphQl(linkService).route ~ GraphiQl.route

  Http().newServerAt("0.0.0.0", config.port).bind(route)
  scribe.info(s"cut-link server started at port ${config.port}")

  override def defaultConfig =
    CutLinkConfig(port = 8080, KafkaConfig(bootstrapServers = ""))
}
