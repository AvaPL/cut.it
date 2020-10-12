package link.store.app

import akka.actor.ActorSystem
import config.Config
import link.store.config.LinkStoreConfig
import link.store.elasticsearch.ElasticConnector
import link.store.flow.SaveLinkFlow
import links.elasticsearch.Index
import logging.Logging
import scribe.Level.Trace
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContextExecutor

object LinkStoreApp extends App with Config[LinkStoreConfig] with Logging {
  implicit val system: ActorSystem                        = ActorSystem("link-store")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val elasticConnector = ElasticConnector(config.elastic)
  val saveLinkFlow = SaveLinkFlow(
    config.kafka.kafkaConsumer,
    elasticConnector.bulkIndexConsumerRecordFlow(Index.linkStoreIndex)
  )

  override def defaultMinimumLoggingLevel = Trace // TODO: Remove, debug only

  override def enableLoggingServer = false // TODO: Remove, debug only
}
