package link.store.flow

import akka.actor.ActorSystem
import akka.kafka.CommitterSettings
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.Committer
import akka.stream.scaladsl.Flow
import kafka.KafkaConnector
import link.store.elasticsearch.ElasticConnector
import links.elasticsearch.Index
import links.kafka.{ConsumerGroup, Topic}
import org.apache.kafka.clients.consumer.ConsumerRecord

case class SaveLinkFlow(
    kafkaConnector: KafkaConnector,
    elasticConnector: ElasticConnector
)(implicit as: ActorSystem) {
  private val consumerSource =
    kafkaConnector.consumer(Topic.linkCutTopic, ConsumerGroup.linkStoreGroup)
  private val indexFlow =
    elasticConnector.bulkIndexConsumerRecordFlow(Index.linkStoreIndex)

  consumerSource
    .via(logRecord)
    .via(indexFlow)
    .runWith(committerSink)

  private def logRecord =
    Flow[(ConsumerRecord[String, String], CommittableOffset)].map {
      case (record, ctx) =>
        scribe.debug(s"Received link from Kafka: ${record.value}")
        (record, ctx)
    }

  private def committerSink =
    Committer.sinkWithOffsetContext(CommitterSettings(as))
}
