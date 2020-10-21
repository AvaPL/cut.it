import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
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
          // TODO: Cleanup
          val kafkaConfig     = KafkaConfig(kafka.bootstrapServers)
          val kafkaConnector  = KafkaConnector(kafkaConfig)
          val linkMessageFlow = LinkMessageFlow(kafkaConnector)
          val linkService     = LinkService(linkMessageFlow)
          val graphQl         = GraphQl(linkService)
          val bulkConfig      = BulkConfig(1000, 10.seconds)
          val elasticConfig =
            ElasticConfig(elasticsearch.httpHostAddress, bulkConfig)
          val elasticConnector = ElasticConnector(elasticConfig)
          SaveLinkFlow(kafkaConnector, elasticConnector)
          val uri = "http://test.com"
          val mutation =
            s"""{
               | "query": "mutation($$uri: String!) { cutLink(uri: $$uri) { id } }",
               | "variables": {
               |   "uri": "$uri"
               | }
               |}
               |""".stripMargin
          val httpEntity = HttpEntity(ContentTypes.`application/json`, mutation)
          val id = Post("/graphql", httpEntity) ~> graphQl.route ~> check {
            status should be(StatusCodes.OK)
            responseAs[Json].hcursor
              .downField("data")
              .downField("cutLink")
              .downField("id")
              .as[String]
              .toOption
              .get
          }
          val linkFuture = EitherT(
            akka.pattern.retry(
              () =>
                elasticConnector
                  .getDocument(Index.linkStoreIndex, id)
                  .map(doc => decode[Link](doc)),
              10,
              1.second
            )(system.dispatcher, system.scheduler)
          ).rethrowT
          val link = Await.result(linkFuture, 10.seconds)
          link.uri should be(uri)
      }
    }
  }
}
