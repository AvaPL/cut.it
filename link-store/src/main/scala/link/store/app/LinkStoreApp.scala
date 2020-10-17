package link.store.app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import config.Config
import link.store.config.LinkStoreConfig
import link.store.elasticsearch.ElasticConnector
import link.store.flow.SaveLinkFlow
import link.store.http.LinkRetrievalService
import links.kafka.KafkaConnector
import logging.Logging
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContextExecutor

object LinkStoreApp extends App with Config[LinkStoreConfig] with Logging {
  implicit val system: ActorSystem                        = ActorSystem("link-store")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val kafkaConnector   = KafkaConnector(config.kafka)
  val elasticConnector = ElasticConnector(config.elastic)
  val saveLinkFlow     = SaveLinkFlow(kafkaConnector, elasticConnector)
  val route            = LinkRetrievalService(elasticConnector).route

  Http().newServerAt("0.0.0.0", config.port).bind(route)
  scribe.info(s"link-store server started at port ${config.port}")
}
