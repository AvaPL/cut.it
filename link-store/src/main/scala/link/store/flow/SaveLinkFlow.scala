package link.store.flow

import akka.actor.ActorSystem
import akka.kafka.CommitterSettings
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.Committer
import akka.stream.scaladsl.{Flow, Source}
import links.kafka.Topic
import org.apache.kafka.clients.consumer.ConsumerRecord

// TODO: Add tests
case class SaveLinkFlow(
    consumerSource: String => Source[
      (ConsumerRecord[String, String], CommittableOffset),
      _
    ],
    indexFlow: Flow[
      (ConsumerRecord[String, String], CommittableOffset),
      (_, CommittableOffset),
      _
    ]
)(implicit as: ActorSystem) {
  consumerSource(Topic.cutLinkTopic)
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
