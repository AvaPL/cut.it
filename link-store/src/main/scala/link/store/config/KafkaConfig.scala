package link.store.config

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.SourceWithContext
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer

case class KafkaConfig(bootstrapServers: String) {
  def kafkaConsumer(
      topic: String
  )(implicit system: ActorSystem): SourceWithContext[
    ConsumerRecord[String, String],
    CommittableOffset,
    Control
  ] =
    Consumer.sourceWithOffsetContext(
      consumerSettings,
      Subscriptions.topics(topic)
    )

  private def consumerSettings(implicit system: ActorSystem) =
    ConsumerSettings(
      system,
      new StringDeserializer,
      new StringDeserializer
    ).withGroupId("link_store")
      .withBootstrapServers(
        bootstrapServers
      ) // TODO: Replace with discovery in the future
}
