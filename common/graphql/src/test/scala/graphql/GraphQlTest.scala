package graphql

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import support.impl.context.UserRepository
import support.impl.model.User
import support.impl.schema.SchemaDefinition

class GraphQlTest extends AnyWordSpec with Matchers with ScalatestRouteTest {
  "GraphQl" when {
    val userRepository = UserRepository()
    val graphql        = GraphQl(SchemaDefinition.schema, userRepository)
    "sending POST to /graphql" should {
      "return entity for query" in {
        val graphqlQuery = createQuery("query { users { username } }")
        val entity       = HttpEntity(ContentTypes.`application/json`, graphqlQuery)
        val expectedData =
          """{
            |  "users": [
            |    {
            |      "username": "Pafeu"
            |    },
            |    {
            |      "username": "Josh"
            |    },
            |    {
            |      "username": "Julia"
            |    }
            |  ]
            |}
            |""".stripMargin

        Post("/graphql", entity) ~> graphql.route ~> check {
          status should be(StatusCodes.OK)
          responseAs[String] should be(responseData(expectedData))
        }
      }

      "return entity for mutation" in {
        val user = User("Adrian", "Hello!")
        val query =
          "mutation($userInput: UserInput!) { createUser(userInput: $userInput) { username, status } }"
        val variables =
          s"""{
             |  "userInput": {
             |    "username": "${user.username}",
             |    "status": "${user.status}"
             |  }
             |}
             |""".stripMargin
        val graphqlQuery = createQuery(query, variables = Some(variables))
        val entity       = HttpEntity(ContentTypes.`application/json`, graphqlQuery)
        val expectedData =
          s"""{
             |  "createUser": {
             |    "username": "${user.username}",
             |    "status": "${user.status}"
             |  }
             |}
             |"""

        Post("/graphql", entity) ~> graphql.route ~> check {
          status should be(StatusCodes.OK)
          responseAs[String] should be(responseData(expectedData))
        }
        userRepository.deleteUser(user.username)
      }

      "choose correct operation" in {
        val query =
          "query execute { users { username } } query dontExecute { users { status } }"
        val operationName = "execute"
        val graphqlQuery =
          createQuery(query, operationName = Some(operationName))
        val entity = HttpEntity(ContentTypes.`application/json`, graphqlQuery)
        val expectedData =
          """{
            |  "users": [
            |    {
            |      "username": "Pafeu"
            |    },
            |    {
            |      "username": "Josh"
            |    },
            |    {
            |      "username": "Julia"
            |    }
            |  ]
            |}
            |""".stripMargin

        Post("/graphql", entity) ~> graphql.route ~> check {
          status should be(StatusCodes.OK)
          responseAs[String] should be(responseData(expectedData))
        }
      }

      "respond with BadRequest for malformed query" in {
        val query =
          """{
            | "query": "query { invalid }"
            |}
            |""".stripMargin
        val entity = HttpEntity(ContentTypes.`application/json`, query)

        Post("/graphql", entity) ~> graphql.route ~> check {
          status should be(StatusCodes.BadRequest)
        }
      }
    }
  }

  private def createQuery(
      query: String,
      variables: Option[String] = None,
      operationName: Option[String] = None
  ) = {
    val variablesField = variables
      .map(variables => s""","variables": $variables""")
      .getOrElse("")
    val operationNameField = operationName
      .map(operationName => s""","operationName": "$operationName"""")
      .getOrElse("")
    s"""{
       | "query": "$query"
       | $variablesField
       | $operationNameField
       |}
       |""".stripMargin
  }

  private def responseData(data: String) =
    s"""{
       |  "data": $data
       |}
       |""".stripMargin.replaceAll("\\s", "")
}
