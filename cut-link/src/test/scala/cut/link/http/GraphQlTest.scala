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

  val testUri = "http://test.com"

  "GraphQl" when {
    "sending POST to /graphql" should {
      "respond with 200 OK and return entity" in {
        val graphQl                     = mockedGraphQl
        val (request, expectedResponse) = testCutLinkMutation
        val requestEntity               = HttpEntity(ContentTypes.`application/json`, request)

        Post("/graphql", requestEntity) ~> graphQl.route ~> check {
          status should be(StatusCodes.OK)
          responseAs[String] should be(expectedResponse)
        }
      }
    }
  }

  private def mockedGraphQl = {
    val mockKafkaConnector = mock[KafkaConnector]
    (mockKafkaConnector
      .producer(_: ActorSystem))
      .expects(*)
      .returning(Sink.ignore)
    val ignoreFlow  = LinkMessageFlow(mockKafkaConnector)
    val linkService = LinkService(ignoreFlow)
    GraphQl(linkService)
  }

  private def testCutLinkMutation = {
    val mutation =
      s"""{
         | "query": "mutation($$uri: String!) { cutLink(uri: $$uri) { uri } }",
         | "variables": {
         |   "uri": "$testUri"
         | }
         |}
         |""".stripMargin
    val response =
      s"""
         |{
         |  "data": {
         |    "cutLink": {
         |      "uri": "$testUri"
         |    }
         |  }
         |}
         |""".stripMargin.replaceAll("\\s", "")
    (mutation, response)
  }
}
