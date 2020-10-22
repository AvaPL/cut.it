package link.store.http

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.MethodRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Sink
import akka.testkit.TestProbe
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser.decode
import link.store.elasticsearch.ElasticConnector
import link.store.flow.LinkRetrievedMessageFlow
import links.elasticsearch.Index
import links.kafka.KafkaConnector
import links.model.Link
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future
import scala.concurrent.duration._

class LinkRetrievalServiceTest
    extends AnyWordSpec
    with Matchers
    with ScalatestRouteTest
    with MockFactory {

  val testProbe: TestProbe = TestProbe()
  val linkRetrievedMessageFlow: LinkRetrievedMessageFlow =
    mockRetrievedMessageFlow(testProbe)

  "LinkRetrievalService" when {
    "given a correct link id" should {
      "redirect user to link uri" in {
        val link         = Link("testId", "https://github.com/AvaPL")
        val linkDocument = link.asJson.noSpaces
        val linkRetrievalService =
          serviceWithGetDocumentSuccess(link.id, linkDocument)

        Get(s"/${link.id}") ~> linkRetrievalService.route ~> check {
          status should be(StatusCodes.MovedPermanently)
          header(Location.name).map(_.value) should contain(link.uri)
        }
        checkLinkMessageSent(link, testProbe)
      }

      "respond with 404 Not Found for decoding failure" in {
        val linkId            = "testId"
        val malformedDocument = "malformed JSON link"
        val linkRetrievalService =
          serviceWithGetDocumentSuccess(linkId, malformedDocument)

        Get(s"/$linkId") ~> linkRetrievalService.route ~> check {
          status should be(StatusCodes.NotFound)
          header(Location.name) shouldBe empty
          responseAs[String] should not be empty
        }
        testProbe.expectNoMessage(1.second)
      }
    }

    "given invalid link id" should {
      "respond with 404 Not Found and exception message" in {
        val linkId    = "invalidId"
        val exception = new RuntimeException("Exception message")
        val linkRetrievalService: LinkRetrievalService =
          serviceWithGetDocumentFailure(linkId, exception)

        Get(s"/$linkId") ~> linkRetrievalService.route ~> check {
          status should be(StatusCodes.NotFound)
          header(Location.name) shouldBe empty
          responseAs[String] should be(exception.getMessage)
        }
        testProbe.expectNoMessage(1.second)
      }
    }

    "called with invalid number of segments" should {
      val linkRetrievalService = mockLinkRetrievalService

      "reject request for more than 1 segment" in {
        Get("/more/segments") ~> linkRetrievalService.route ~> check {
          handled should be(false)
        }
      }

      "reject request for no segments" in {
        Get() ~> linkRetrievalService.route ~> check {
          handled should be(false)
        }
      }
    }

    "called with not allowed methods" should {
      "reject request" in {
        val linkRetrievalService = mockLinkRetrievalService

        Post("/id") ~> linkRetrievalService.route ~> check {
          rejection shouldBe a[MethodRejection]
        }
        Put("/id") ~> linkRetrievalService.route ~> check {
          rejection shouldBe a[MethodRejection]
        }
        Patch("/id") ~> linkRetrievalService.route ~> check {
          rejection shouldBe a[MethodRejection]
        }
        Delete("/id") ~> linkRetrievalService.route ~> check {
          rejection shouldBe a[MethodRejection]
        }
      }
    }
  }

  private def mockRetrievedMessageFlow(testProbe: TestProbe) = {
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

  private def serviceWithGetDocumentSuccess(id: String, document: String) = {
    val mockElasticConnector = mock[ElasticConnector]
    (mockElasticConnector.getDocument _)
      .expects(Index.linkStoreIndex, id)
      .returning(Future.successful(document))
    val linkRetrievalService = LinkRetrievalService(
      mockElasticConnector,
      linkRetrievedMessageFlow
    )
    linkRetrievalService
  }

  private def checkLinkMessageSent(link: Link, testProbe: TestProbe) = {
    val message = testProbe.receiveOne(1.second)
    val record  = message.asInstanceOf[ProducerRecord[String, String]]
    val messageLink =
      decode[Link](record.value).getOrElse(throw new RuntimeException)
    messageLink should be(link)
  }

  private def serviceWithGetDocumentFailure(
      linkId: String,
      exception: RuntimeException
  ) = {
    val mockElasticConnector = mock[ElasticConnector]
    (mockElasticConnector.getDocument _)
      .expects(Index.linkStoreIndex, linkId)
      .returning(Future.failed(exception))
    val linkRetrievalService = LinkRetrievalService(
      mockElasticConnector,
      linkRetrievedMessageFlow
    )
    linkRetrievalService
  }

  private def mockLinkRetrievalService = {
    val mockElasticConnector = mock[ElasticConnector]
    val linkRetrievalService =
      LinkRetrievalService(mockElasticConnector, linkRetrievedMessageFlow)
    linkRetrievalService
  }
}
