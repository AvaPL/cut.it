package links.kafka

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.dimafeng.testcontainers.{ForAllTestContainer, KafkaContainer}
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration._

class KafkaConnectorTest
    extends AnyWordSpec
    with Matchers
    with ForAllTestContainer {
  implicit val system: ActorSystem = ActorSystem("test")

  override def container: KafkaContainer = KafkaContainer()

  "KafkaConnector" when {
    "given message is sent to producer" should {
      "receive sent message in consumer" in {
        val kafkaConfig          = KafkaConfig(container.bootstrapServers)
        val kafkaConnector       = KafkaConnector(kafkaConfig)
        val topic                = "testTopic"
        val producer             = kafkaConnector.producer
        val consumer             = kafkaConnector.consumer(topic)
        val producerConsumerFlow = Flow.fromSinkAndSource(producer, consumer)
        val testMessage          = new ProducerRecord(topic, "key", "value")

        val messageFuture = Source
          .single(testMessage)
          .via(producerConsumerFlow)
          .runWith(Sink.head)
        val message = Await.result(messageFuture, 10.seconds)._1

        message.key should be(testMessage.key)
        message.value should be(testMessage.value)
      }
    }
  }
}
