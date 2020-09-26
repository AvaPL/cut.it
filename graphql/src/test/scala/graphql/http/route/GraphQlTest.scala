package graphql.http.route

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import graphql.service.LinkService
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GraphQlTest extends AnyWordSpec with Matchers with ScalatestRouteTest {
  "GraphQl" when {
    "sending POST to /graphql" should {
      "respond with 200 OK and return entity" in {
        val linkService = LinkService()
        val graphql     = GraphQl(linkService)
        val query =
          """{
            | "query": "query($id: String!) { uncutLink(id: $id) { id } }"
            | "variables": {
            |   "id": "123abc"
            | }
            |}
            |""".stripMargin
        val entity = HttpEntity(ContentTypes.`application/json`, query)
        val expectedEntity =
          """
            |{
            |  "data": {
            |    "uncutLink": {
            |      "id": "123abc"
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
