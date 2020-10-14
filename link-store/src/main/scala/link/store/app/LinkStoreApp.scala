package link.store.app

import akka.actor.ActorSystem
import config.Config
import link.store.config.LinkStoreConfig
import link.store.elasticsearch.ElasticConnector
import link.store.flow.SaveLinkFlow
import links.elasticsearch.Index
import links.kafka.{KafkaConnector, Topic}
import logging.Logging
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContextExecutor

object LinkStoreApp extends App with Config[LinkStoreConfig] with Logging {
  implicit val system: ActorSystem                        = ActorSystem("link-store")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val elasticConnector = ElasticConnector(config.elastic)
  val kafkaConsumer    = KafkaConnector(config.kafka).consumer(Topic.cutLinkTopic)
  val indexFlow =
    elasticConnector.bulkIndexConsumerRecordFlow(Index.linkStoreIndex)
  val saveLinkFlow = SaveLinkFlow(kafkaConsumer, indexFlow)
}
