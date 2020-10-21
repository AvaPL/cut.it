package links.kafka

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.dimafeng.testcontainers.KafkaContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration._

class KafkaConnectorTest
    extends AnyWordSpec
    with Matchers
    with TestContainerForAll {
  implicit val system: ActorSystem = ActorSystem("test")

  override val containerDef: KafkaContainer.Def = KafkaContainer.Def()

  "KafkaConnector" when {
    "given message is sent to producer" should {
      "receive sent message in consumer" in withContainers { container =>
        val topic       = "testTopic"
        val testMessage = new ProducerRecord(topic, "key", "value")
        val messageFlow =
          producerConsumerFlow(container.bootstrapServers, topic)

        val messageFuture =
          Source.single(testMessage).via(messageFlow).runWith(Sink.head)
        val message = Await.result(messageFuture, 10.seconds)._1

        message.key should be(testMessage.key)
        message.value should be(testMessage.value)
      }
    }
  }

  private def producerConsumerFlow(bootstrapServers: String, topic: String) = {
    val kafkaConfig    = KafkaConfig(bootstrapServers)
    val kafkaConnector = KafkaConnector(kafkaConfig)
    val producer       = kafkaConnector.producer
    val consumer       = kafkaConnector.consumer(topic, "test-group")
    Flow.fromSinkAndSource(producer, consumer)
  }
}
