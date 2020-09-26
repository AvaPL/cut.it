package basic.graphql.http.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GraphiQlTest extends AnyWordSpec with Matchers with ScalatestRouteTest {
  "GraphiQl" when {
    "sending GET to /graphiql" should {
      "respond with 200 OK" in {
        Get("/graphiql") ~> GraphiQl.route ~> check {
          status should be(StatusCodes.OK)
        }
      }
    }
  }
}
