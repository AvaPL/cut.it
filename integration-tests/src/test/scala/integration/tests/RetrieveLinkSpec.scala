package integration.tests

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.stream.scaladsl.Sink
import com.dimafeng.testcontainers.lifecycle._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import integration.tests.common.IntegrationTest
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import link.store.elasticsearch.ElasticConnector
import link.store.flow.LinkRetrievedMessageFlow
import link.store.http.LinkRetrievalService
import links.elasticsearch.Index
import links.kafka.{KafkaConnector, Topic}
import links.model.Link

import scala.concurrent.Await
import scala.concurrent.duration._

class RetrieveLinkSpec extends IntegrationTest {
  "Retrieve link flow" when {
    "a retrieve link request is sent" should {
      "retrieve link and send a link retrieved event message" in withContainers {
        case kafka and elasticsearch =>
          val kafkaConnector   = testKafkaConnector(kafka)
          val elasticConnector = testElasticConnector(elasticsearch)
          val testLink         = Link("testId", "https://github.com/AvaPL")

          indexLink(elasticConnector, testLink)
          val linkRetrievalService =
            startLinkRetrievedFlow(kafkaConnector, elasticConnector)
          sendRetrievalRequest(linkRetrievalService, testLink)
          val eventLink = receiveLinkMessage(kafkaConnector)

          eventLink should be(testLink)
      }
    }
  }

  private def indexLink(elasticConnector: ElasticConnector, testLink: Link) = {
    val refreshFuture = elasticConnector.client.execute {
      indexInto(Index.linkStoreIndex)
        .withId(testLink.id)
        .doc(testLink.asJson.noSpaces)
        .refresh(RefreshPolicy.WAIT_FOR)
    }
    Await.ready(refreshFuture, 10.seconds)
  }

  private def startLinkRetrievedFlow(
      kafkaConnector: KafkaConnector,
      elasticConnector: ElasticConnector
  ) = {
    val linkRetrievedMessageFlow =
      LinkRetrievedMessageFlow(kafkaConnector)
    val linkRetrievalService =
      LinkRetrievalService(elasticConnector, linkRetrievedMessageFlow)
    linkRetrievalService
  }

  private def sendRetrievalRequest(
      linkRetrievalService: LinkRetrievalService,
      testLink: Link
  ) =
    Get(s"/${testLink.id}") ~> linkRetrievalService.route ~> check {
      status should be(StatusCodes.MovedPermanently)
      header(Location.name).map(_.value) should contain(testLink.uri)
    }

  private def receiveLinkMessage(kafkaConnector: KafkaConnector) = {
    val linkRetrievedEventFuture = kafkaConnector
      .consumer(Topic.linkRetrievedTopic, "test_group")
      .runWith(Sink.head)
    val linkEventMessage = Await.result(linkRetrievedEventFuture, 10.seconds)
    decode[Link](linkEventMessage._1.value).toOption.get
  }
}
