package basic.graphql.http.route

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import basic.graphql.repository.UserRepository
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GraphQlTest extends AnyWordSpec with Matchers with ScalatestRouteTest {
  "GraphQl" when {
    "sending POST to /graphql" should {
      "respond with 200 OK and return entity" in {
        val userRepository = UserRepository()
        val graphql        = GraphQl(userRepository)
        val query =
          """{
            | "query": "query { users { username } }"
            |}
            |""".stripMargin
        val entity = HttpEntity(ContentTypes.`application/json`, query)
        val expectedEntity =
          """
            |{
            |  "data": {
            |    "users": [
            |      {
            |        "username": "Pafeu"
            |      },
            |      {
            |        "username": "Josh"
            |      },
            |      {
            |        "username": "Julia"
            |      }
            |    ]
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
