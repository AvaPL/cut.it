package cut.link.config

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Flow, Keep, Sink}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

case class KafkaConfig(bootstrapServers: String) {
  def kafkaProducer(implicit
      system: ActorSystem
  ): Sink[ProducerRecord[String, String], Object] =
    createSink(producerSettings)

  private def producerSettings(implicit system: ActorSystem) =
    ProducerSettings(
      system,
      new StringSerializer,
      new StringSerializer
    ).withBootstrapServers(
      bootstrapServers
    ) // TODO: Replace with discovery in the future

  private def createSink(producerSettings: ProducerSettings[String, String]) =
    if (bootstrapServers.isBlank) {
      scribe.error(
        "No Kafka bootstrap servers provided, messages won't be sent"
      )
      ignoreMessagesSink
    } else
      Producer.plainSink(producerSettings)

  private def ignoreMessagesSink =
    Flow[ProducerRecord[String, String]].toMat(
      Sink.foreach(_ =>
        scribe.warn(s"Kafka message not sent, the producer is missing")
      )
    )(Keep.right)
}
