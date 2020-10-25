package link.store.flow

import java.time.OffsetDateTime

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.testkit.TestProbe
import io.circe.generic.auto._
import io.circe.parser._
import kafka.KafkaConnector
import links.model.Link
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future
import scala.concurrent.duration._

class LinkRetrievedMessageFlowTest
    extends AnyWordSpec
    with Matchers
    with MockFactory {
  implicit val system: ActorSystem = ActorSystem("test")

  "LinkRetrievedMessageFlow" when {
    "given a link to send" should {
      "send Kafka message" in {
        val link                     = testLink
        val testProbe                = TestProbe()
        val linkRetrievedMessageFlow = mockedLinkRetrievedMessageFlow(testProbe)

        linkRetrievedMessageFlow.sendLinkRetrievedMessage(link)
        val messageLink: Link = receiveLink(testProbe)

        messageLink should be(link)
      }
    }
  }

  private def testLink =
    Link(
      id = "id",
      uri = "http://test.com",
      created = OffsetDateTime.parse("2020-10-11T12:34:56Z")
    )

  private def mockedLinkRetrievedMessageFlow(testProbe: TestProbe) = {
    val testSink = Sink
      .actorRef(testProbe.ref, "completed", _ => "failure")
      .mapMaterializedValue(_ => Future.successful(Done))
    val mockKafkaConnector = mock[KafkaConnector]
    (mockKafkaConnector
      .producer(_: ActorSystem))
      .expects(*)
      .returning(testSink)
    val linkRetrievedMessageFlow =
      LinkRetrievedMessageFlow(mockKafkaConnector)
    linkRetrievedMessageFlow
  }

  private def receiveLink(testProbe: TestProbe) = {
    val message = testProbe.receiveOne(1.second)
    val record  = message.asInstanceOf[ProducerRecord[String, String]]
    val messageLink =
      decode[Link](record.value).getOrElse(throw new RuntimeException)
    messageLink
  }
}
