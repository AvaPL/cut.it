package cut.link.app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import config.Config
import cut.link.config.CutLinkConfig
import cut.link.flow.LinkMessageFlow
import cut.link.schema.SchemaDefinition
import cut.link.service.LinkService
import graphql.{GraphQl, GraphiQl}
import kafka.KafkaConnector
import logging.Logging
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContextExecutor

object CutLinkApp extends App with Config[CutLinkConfig] with Logging {
  implicit val system: ActorSystem                        = ActorSystem("cut-link")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val kafkaConnector  = KafkaConnector(config.kafka)
  val linkMessageFlow = LinkMessageFlow(kafkaConnector)
  val linkService     = LinkService(linkMessageFlow)
  val route =
    GraphQl(SchemaDefinition.schema, linkService).route ~ GraphiQl.route

  Http().newServerAt("0.0.0.0", config.port).bind(route)
  scribe.info(s"cut-link server started at port ${config.port}")

  override def servicePackage = "cut.link"
}
