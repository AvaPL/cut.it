package link.store.flow

import akka.actor.ActorSystem
import akka.kafka.CommitterSettings
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.Committer
import akka.stream.scaladsl.{Flow, Source}
import org.apache.kafka.clients.consumer.ConsumerRecord

case class SaveLinkFlow(
    consumerSource: Source[
      (ConsumerRecord[String, String], CommittableOffset),
      _
    ],
    indexFlow: Flow[
      (ConsumerRecord[String, String], CommittableOffset),
      (_, CommittableOffset),
      _
    ]
)(implicit as: ActorSystem) {
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
