package cut.link.flow

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.testkit.TestProbe
import cut.link.model.Link
import io.circe.generic.auto._
import io.circe.parser._
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class LinkMessageFlowTest extends AnyWordSpec with Matchers {
  implicit val system: ActorSystem = ActorSystem("test")

  "LinkMessageFlow" when {
    "given a link to send" should {
      "send Kafka message" in {
        val link = Link(
          id = "id",
          uri = "http://test.com",
          created = OffsetDateTime.parse("2020-10-11T12:34:56Z")
        )
        val testProbe       = TestProbe()
        val testSink        = Sink.actorRef(testProbe.ref, "completed", _ => "failure")
        val linkMessageFlow = LinkMessageFlow("testTopic", testSink)

        linkMessageFlow.sendLinkMessage(link)
        val message = testProbe.receiveOne(1.second)
        val record  = message.asInstanceOf[ProducerRecord[String, String]]
        val messageLink =
          decode[Link](record.value).getOrElse(throw new RuntimeException)

        messageLink should be(link)
      }
    }
  }
}
