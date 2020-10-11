package cut.link.http

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Sink
import cut.link.flow.LinkMessageFlow
import cut.link.service.LinkService
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GraphQlTest extends AnyWordSpec with Matchers with ScalatestRouteTest {
  "GraphQl" when {
    "sending POST to /graphql" should {
      "respond with 200 OK and return entity" in {
        val ignoreFlow  = LinkMessageFlow(Sink.ignore)
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
