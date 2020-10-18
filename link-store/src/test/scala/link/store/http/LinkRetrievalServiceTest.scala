package link.store.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.MethodRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.syntax._
import io.circe.generic.auto._
import link.store.elasticsearch.ElasticConnector
import links.elasticsearch.Index
import links.model.Link
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class LinkRetrievalServiceTest
    extends AnyWordSpec
    with Matchers
    with ScalatestRouteTest
    with MockFactory {

  "LinkRetrievalService" when {
    "given a correct link id" should {
      "redirect user to link uri" in {
        val link                 = Link("testId", "https://github.com/AvaPL")
        val linkDocument         = link.asJson.noSpaces
        val mockElasticConnector = mock[ElasticConnector]
        (mockElasticConnector.getDocument _)
          .expects(Index.linkStoreIndex, link.id)
          .returning(Future.successful(linkDocument))
        val linkRetrievalService = LinkRetrievalService(mockElasticConnector)

        Get(s"/${link.id}") ~> linkRetrievalService.route ~> check {
          status should be(StatusCodes.MovedPermanently)
          header(Location.name).map(_.value) should contain(link.uri)
        }
      }

      "respond with 404 Not Found for decoding failure" in {
        val linkId               = "testId"
        val malformedDocument    = "malformed JSON link"
        val mockElasticConnector = mock[ElasticConnector]
        (mockElasticConnector.getDocument _)
          .expects(Index.linkStoreIndex, linkId)
          .returning(Future.successful(malformedDocument))
        val linkRetrievalService = LinkRetrievalService(mockElasticConnector)

        Get(s"/$linkId") ~> linkRetrievalService.route ~> check {
          status should be(StatusCodes.NotFound)
          header(Location.name) shouldBe empty
          responseAs[String] should not be empty
        }
      }
    }

    "given invalid link id" should {
      "respond with 404 Not Found and exception message" in {
        val linkId               = "invalidId"
        val exception            = new RuntimeException("Exception message")
        val mockElasticConnector = mock[ElasticConnector]
        (mockElasticConnector.getDocument _)
          .expects(Index.linkStoreIndex, linkId)
          .returning(Future.failed(exception))
        val linkRetrievalService = LinkRetrievalService(mockElasticConnector)

        Get(s"/$linkId") ~> linkRetrievalService.route ~> check {
          status should be(StatusCodes.NotFound)
          header(Location.name) shouldBe empty
          responseAs[String] should be(exception.getMessage)
        }
      }
    }

    "called with invalid number of segments" should {
      val mockElasticConnector = mock[ElasticConnector]
      val linkRetrievalService = LinkRetrievalService(mockElasticConnector)

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
        val mockElasticConnector = mock[ElasticConnector]
        val linkRetrievalService = LinkRetrievalService(mockElasticConnector)

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
}
