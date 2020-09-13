package schema

import io.circe.generic.auto._
import io.circe.syntax._
import model.User
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import repository.UserRepository
import sangria.execution.Executor
import sangria.macros._
import sangria.marshalling.circe._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SchemaDefinitionTest extends AnyWordSpec with Matchers {
  "SchemaDefinition" when {
    val schema = SchemaDefinition.schema
    val userRepository = UserRepository()

    "queried for users" should {
      "return defined users list" in {
        val query =
          graphql"""
            query {
              users {
                username,
                status
              }
            }
                 """

        val future = Executor
          .execute(schema, query, userRepository)
          .map(_.as[List[User]])
        val users = Await.result(future, 10.seconds)

        users should be(userRepository.users)
      }

      "return existing user" in {
        val query =
          graphql"""
            query($$username: String!) {
              user(username: $$username){
                username,
                status
              }
            }
                 """
        val variables =
          """
            {
              "username": "Pafeu"
            }
          """.asJson

        val future = Executor
          .execute(
            schema,
            query,
            variables = variables,
            userContext = userRepository
          )
          .map(_.as[Option[User]])
        val user = Await.result(future, 10.seconds)

        user should be(userRepository.user("Pafeu"))
      }
    }
  }
}
