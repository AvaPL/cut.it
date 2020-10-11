package link.store.flow

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.stream.scaladsl.{Flow, Sink, SourceWithContext}
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
  //  Take message from Kafka
  //  Parse to Link
  //  Serialize to Elasticsearch object
  //  Save to Elasticsearch
  //  + add appropriate logging on each step

  val cutLinkTopic = "cut_link" // TODO: Move this topic to a common module

  val saveLinkFlow: Future[Done] =
    consumerSource(cutLinkTopic).via(logRecord).runWith(Sink.ignore)

  private def logRecord = Flow[(ConsumerRecord[String, String], _)].map {
    case (record, ctx) =>
      scribe.debug(s"Received link from Kafka: ${record.value}")
      (record, ctx)
  }
}
