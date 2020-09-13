package schema

import io.circe.{Decoder, Json}
import io.circe.generic.auto._
import io.circe.parser._
import model.User
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import repository.UserRepository
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros._
import sangria.marshalling.circe._
import sangria.schema.Schema

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SchemaDefinitionTest extends AnyWordSpec with Matchers {
  val schema: Schema[UserRepository, Unit] = SchemaDefinition.schema
  val userRepository: UserRepository = UserRepository()

  "SchemaDefinition" when {
    "queried for users" should {

      val allUsersQuery =
        graphql"""
            query {
              users {
                username,
                status
              }
            }
          """

      "return defined users list" in {

        val future = query(allUsersQuery)
          .map(parseQueryData[List[User]](_, "users"))
        val users = Await.result(future, 10.seconds)

        users should be(userRepository.users)
      }

      val userQuery =
        graphql"""
            query($$username: String!) {
              user(username: $$username) {
                username,
                status
              }
            }
          """

      "return existing user" in {
        val variables = usernameVariables("Pafeu")

        val future = query(userQuery, variables)
          .map(parseQueryData[Option[User]](_, "user"))
        val user = Await.result(future, 10.seconds)

        user should be(userRepository.user("Pafeu"))
      }

      "return null for nonexistent user" in {
        val variables = usernameVariables("Nonexistent")

        val future = query(userQuery, variables)
          .map(parseQueryData[Option[User]](_, "user"))
        val user = Await.result(future, 10.seconds)

        user should be(None)
      }
    }
  }

  private def query(userQuery: Document, variables: Json = Json.obj()) = {
    Executor
      .execute(
        schema = schema,
        queryAst = userQuery,
        variables = variables,
        userContext = userRepository
      )
  }

  private def parseQueryData[T](json: Json, field: String)(implicit
      decoder: Decoder[T]
  ) = {
    json.hcursor.downField("data").get[T](field) match {
      case Right(value)  => value
      case Left(failure) => throw new RuntimeException(failure.message)
    }
  }

  private def usernameVariables(username: String) = {
    json(
      s"""
            {
              "username": "$username"
            }
      """
    )
  }

  private def json(string: String) = {
    parse(string) match {
      case Right(variables) => variables
      case Left(failure)    => throw new RuntimeException(failure.message)
    }
  }
}
