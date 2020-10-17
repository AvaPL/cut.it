package cut.link.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import cut.link.flow.LinkMessageFlow
import links.kafka.KafkaConnector
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LinkServiceTest extends AnyWordSpec with Matchers with MockFactory {
  implicit val system: ActorSystem = ActorSystem("test")

  "LinkService" when {
    "cutLink is called with uri" should {
      val uri                = "http://test.com"
      val mockKafkaConnector = mock[KafkaConnector]
      (mockKafkaConnector
        .producer(_: ActorSystem))
        .expects(*)
        .returning(Sink.ignore)
      val ignoreFlow  = LinkMessageFlow(mockKafkaConnector)
      val linkService = LinkService(ignoreFlow)

      "return cut link" in {
        val link = linkService.cutLink(uri)

        link.id should not be empty
        link.uri should be(uri)
      }

      "return id without leading '='" in {
        val link = linkService.cutLink(uri)

        link.id should not startWith "="
      }
    }
  }
}
