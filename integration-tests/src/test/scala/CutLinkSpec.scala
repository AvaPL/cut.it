import akka.actor.Scheduler
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.retry
import cats.data.EitherT
import cats.implicits._
import com.dimafeng.testcontainers.lifecycle._
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import com.dimafeng.testcontainers.{ElasticsearchContainer, KafkaContainer}
import cut.link.flow.LinkMessageFlow
import cut.link.http.GraphQl
import cut.link.model.Link
import cut.link.service.LinkService
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.decode
import link.store.config.{BulkConfig, ElasticConfig}
import link.store.elasticsearch.ElasticConnector
import link.store.flow.SaveLinkFlow
import links.elasticsearch.Index
import links.kafka.{KafkaConfig, KafkaConnector}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration._

class CutLinkSpec
    extends AnyWordSpec
    with Matchers
    with TestContainersForAll
    with ScalatestRouteTest {
  override type Containers = KafkaContainer and ElasticsearchContainer

  override def startContainers(): Containers = {
    val kafka = KafkaContainer.Def().start()
    val elasticsearch =
      ElasticsearchContainer.Def("bitnami/elasticsearch").start()
    kafka and elasticsearch
  }

  "Cut link flow" when {
    "a cut link request is sent" should {
      "send link to Elasticsearch" in withContainers {
        case kafka and elasticsearch =>
          val kafkaConnector   = testKafkaConnector(kafka)
          val graphQl          = testGraphQl(kafkaConnector)
          val elasticConnector = testElasticConnector(elasticsearch)
          SaveLinkFlow(kafkaConnector, elasticConnector)
          val uri = "http://test.com"

          val createdLinkId = sendGraphQlQuery(graphQl, uri)
          val retrievedLink =
            retrieveEsLinkWithRetries(elasticConnector, createdLinkId)

          retrievedLink.uri should be(uri)
      }
    }
  }

  private def testKafkaConnector(kafka: KafkaContainer) = {
    val kafkaConfig = KafkaConfig(kafka.bootstrapServers)
    KafkaConnector(kafkaConfig)
  }

  private def testElasticConnector(elasticsearch: ElasticsearchContainer) = {
    val bulkConfig    = BulkConfig(1000, 10.seconds)
    val elasticConfig = ElasticConfig(elasticsearch.httpHostAddress, bulkConfig)
    ElasticConnector(elasticConfig)
  }

  private def testGraphQl(kafkaConnector: KafkaConnector) = {
    val linkMessageFlow = LinkMessageFlow(kafkaConnector)
    val linkService     = LinkService(linkMessageFlow)
    GraphQl(linkService)
  }

  private def sendGraphQlQuery(graphQl: GraphQl, uri: String) = {
    val mutation   = cutLinkMutation(uri)
    val httpEntity = HttpEntity(ContentTypes.`application/json`, mutation)
    Post("/graphql", httpEntity) ~> graphQl.route ~> check {
      status should be(StatusCodes.OK)
      returnLinkId
    }
  }

  private def cutLinkMutation(uri: String) =
    s"""{
       | "query": "mutation($$uri: String!) { cutLink(uri: $$uri) { id } }",
       | "variables": {
       |   "uri": "$uri"
       | }
       |}
       |""".stripMargin

  private def returnLinkId =
    responseAs[Json].hcursor
      .downField("data")
      .downField("cutLink")
      .downField("id")
      .as[String]
      .toOption
      .get

  private def retrieveEsLinkWithRetries(
      elasticConnector: ElasticConnector,
      documentId: String
  ) = {
    implicit val scheduler: Scheduler = system.scheduler
    val linkFuture = EitherT(
      retry(() => retrieveEsLink(elasticConnector, documentId), 10, 1.second)
    ).rethrowT
    val retrievedLink = Await.result(linkFuture, 10.seconds)
    retrievedLink
  }

  private def retrieveEsLink(
      elasticConnector: ElasticConnector,
      documentId: String
  ) =
    elasticConnector
      .getDocument(Index.linkStoreIndex, documentId)
      .map(doc => decode[Link](doc))
}
