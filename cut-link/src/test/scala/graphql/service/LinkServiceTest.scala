package graphql.service

import java.time.OffsetDateTime

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.testkit.TestProbe
import graphql.model.Link
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.parser._
import io.circe.generic.auto._

import scala.concurrent.duration._

class LinkServiceTest extends AnyWordSpec with Matchers {
  implicit val system: ActorSystem = ActorSystem("test")

  "LinkService" when {
    "cutLink is called with uri" should {
      val uri = "http://test.com"

      "return cut link" in {
        val linkService = LinkService(Sink.ignore)

        val link = linkService.cutLink(uri)

        validateLink(link, uri)
      }

      "return id without leading '='" in {
        val linkService = LinkService(Sink.ignore)

        val link = linkService.cutLink(uri)

        link.id should not startWith "="
      }

      "send Kafka message" in {
        val testProbe   = TestProbe()
        val testSink    = Sink.actorRef(testProbe.ref, "completed", _ => "failure")
        val linkService = LinkService(testSink)

        linkService.cutLink(uri)
        val message = testProbe.receiveOne(1.second)
        val record  = message.asInstanceOf[ProducerRecord[String, String]]
        val link =
          decode[Link](record.value).getOrElse(throw new RuntimeException)

        validateLink(link, uri)
      }
    }
  }

  private def validateLink(link: Link, uri: String) = {
    link.id should not be empty
    link.uri should be(uri)
    link.created.isBefore(OffsetDateTime.now()) should be(true)
  }
}
