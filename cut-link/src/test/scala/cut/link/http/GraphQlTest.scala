package cut.link.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Sink
import cut.link.flow.LinkMessageFlow
import cut.link.service.LinkService
import links.kafka.KafkaConnector
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GraphQlTest
    extends AnyWordSpec
    with Matchers
    with ScalatestRouteTest
    with MockFactory {
  "GraphQl" when {
    "sending POST to /graphql" should {
      "respond with 200 OK and return entity" in {
        val mockKafkaConnector = mock[KafkaConnector]
        (mockKafkaConnector
          .producer(_: ActorSystem))
          .expects(*)
          .returning(Sink.ignore)
        val ignoreFlow  = LinkMessageFlow(mockKafkaConnector)
        val linkService = LinkService(ignoreFlow)
        val graphql     = GraphQl(linkService)
        val uri         = "http://test.com"
        val query =
          s"""{
             | "query": "mutation($$uri: String!) { cutLink(uri: $$uri) { uri } }",
             | "variables": {
             |   "uri": "$uri"
             | }
             |}
             |""".stripMargin
        val entity = HttpEntity(ContentTypes.`application/json`, query)
        val expectedEntity =
          s"""
             |{
             |  "data": {
             |    "cutLink": {
             |      "uri": "$uri"
             |    }
             |  }
             |}
             |""".stripMargin.replaceAll("\\s", "")

        Post("/graphql", entity) ~> graphql.route ~> check {
          status should be(StatusCodes.OK)
          responseAs[String] should be(expectedEntity)
        }
      }
    }
  }
}
