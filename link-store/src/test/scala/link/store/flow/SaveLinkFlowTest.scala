package link.store.flow

import akka.actor.ActorSystem
import akka.kafka.testkit.ConsumerResultFactory
import akka.kafka.testkit.scaladsl.ConsumerControlFactory
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.testkit.TestProbe
import link.store.elasticsearch.ElasticConnector
import links.elasticsearch.Index
import links.kafka.{KafkaConnector, Topic}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class SaveLinkFlowTest extends AnyWordSpec with Matchers with MockFactory {
  implicit val system: ActorSystem = ActorSystem("test")

  "SaveLinkFlow" when {
    "a message is sent" should {
      "pass the message unchanged to index flow" in {
        val mockKafkaConnector = mock[KafkaConnector]
        val testMessage = (
          new ConsumerRecord(Topic.cutLinkTopic, 0, 0, "key", "value"),
          ConsumerResultFactory.committableOffset(
            "testGroup",
            "testTopic",
            0,
            0,
            "testMeta"
          )
        )
        val testSource = Source
          .single(testMessage)
          .viaMat(ConsumerControlFactory.controlFlow())(Keep.right)
        (mockKafkaConnector
          .consumer(_: String)(_: ActorSystem))
          .expects(Topic.cutLinkTopic, *)
          .returning(testSource)
        val mockElasticConnector = mock[ElasticConnector]
        val testProbe            = TestProbe()
        val testSink = Sink
          .actorRef(testProbe.ref, "completed", _ => "failure")
        (mockElasticConnector.bulkIndexConsumerRecordFlow _)
          .expects(Index.linkStoreIndex)
          .returning(Flow.fromSinkAndSource(testSink, Source.empty))

        SaveLinkFlow(mockKafkaConnector, mockElasticConnector)

        val message = testProbe.receiveOne(1.second)
        message should be(testMessage)
      }
    }
  }
}
