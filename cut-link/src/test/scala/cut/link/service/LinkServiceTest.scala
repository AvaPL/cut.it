package cut.link.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import cut.link.flow.LinkMessageFlow
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LinkServiceTest extends AnyWordSpec with Matchers {
  implicit val system: ActorSystem = ActorSystem("test")

  "LinkService" when {
    "cutLink is called with uri" should {
      val uri         = "http://test.com"
      val ignoreFlow  = LinkMessageFlow("testTopic", Sink.ignore)
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
