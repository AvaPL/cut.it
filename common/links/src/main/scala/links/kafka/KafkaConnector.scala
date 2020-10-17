package links.kafka

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}

import scala.concurrent.Future

case class KafkaConnector(config: KafkaConfig) {
  def producer(implicit
      as: ActorSystem
  ): Sink[ProducerRecord[String, String], Future[Done]] =
    Producer.plainSink(producerSettings)

  private def producerSettings(implicit as: ActorSystem) =
    ProducerSettings(
      as,
      new StringSerializer,
      new StringSerializer
    ).withBootstrapServers(
      config.bootstrapServers
    ) // TODO: Replace with discovery in the future

  def consumer(topic: String)(implicit
      as: ActorSystem
  ): Source[(ConsumerRecord[String, String], CommittableOffset), Control] =
    Consumer
      .sourceWithOffsetContext(consumerSettings, Subscriptions.topics(topic))
      .asSource

  private def consumerSettings(implicit as: ActorSystem) =
    ConsumerSettings(
      as,
      new StringDeserializer,
      new StringDeserializer
    ).withGroupId("link_store")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withBootstrapServers(
        config.bootstrapServers
      ) // TODO: Replace with discovery in the future
}
