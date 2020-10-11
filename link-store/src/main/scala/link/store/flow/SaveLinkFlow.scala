package link.store.flow

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.CommitterSettings
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.Committer
import akka.stream.scaladsl.{Flow, SourceWithContext}
import links.kafka.Topics
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.Future

case class SaveLinkFlow(
    consumerSource: String => SourceWithContext[
      ConsumerRecord[String, String],
      CommittableOffset,
      _
    ]
)(implicit as: ActorSystem) {
  // TODO:
  //  Take message from Kafka             - DONE
  //  Save message value to Elasticsearch
  //  Commit offset to Kafka              - DONE
  //  + add appropriate logging on each step

  val saveLinkFlow: Future[Done] =
    consumerSource(Topics.cutLinkTopic)
      .via(logRecord)
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
