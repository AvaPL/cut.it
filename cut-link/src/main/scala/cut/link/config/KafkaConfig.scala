package cut.link.config

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

case class KafkaConfig(bootstrapServers: String) {
  def kafkaProducer(implicit
      system: ActorSystem
  ): Sink[ProducerRecord[String, String], Object] =
    Producer.plainSink(producerSettings)

  private def producerSettings(implicit system: ActorSystem) =
    ProducerSettings(
      system,
      new StringSerializer,
      new StringSerializer
    ).withBootstrapServers(
      bootstrapServers
    ) // TODO: Replace with discovery in the future
}
