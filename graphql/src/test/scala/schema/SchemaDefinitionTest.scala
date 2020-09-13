package schema

import io.circe.{Decoder, Json}
import io.circe.generic.auto._
import io.circe.parser._
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
          .execute(
            schema = schema,
            queryAst = query,
            userContext = userRepository
          )
          .map(parseQueryData[List[User]](_, "users"))
        val users = Await.result(future, 10.seconds)

        users should be(userRepository.users)
      }

      "return existing user" in {
        val query =
          graphql"""
            query($$username: String!) {
              user(username: $$username) {
                username,
                status
              }
            }
          """
        val variables = json(
          """
            {
              "username": "Pafeu"
            }
          """
        )

        val future = Executor
          .execute(
            schema = schema,
            queryAst = query,
            variables = variables,
            userContext = userRepository
          )
          .map(parseQueryData[Option[User]](_, "user"))
        val user = Await.result(future, 10.seconds)

        user should be(userRepository.user("Pafeu"))
      }

      "return empty json for nonexistent user" in {
        val query =
          graphql"""
            query($$username: String!) {
              user(username: $$username) {
                username,
                status
              }
            }
          """
        val variables = json(
          """
            {
              "username": "Not exists"
            }
          """
        )

        val future = Executor
          .execute(
            schema = schema,
            queryAst = query,
            variables = variables,
            userContext = userRepository
          )
          .map(parseQueryData[Option[User]](_, "user"))
        val user = Await.result(future, 10.seconds)

        user should be(None)
      }
    }
  }

  private def parseQueryData[T](json: Json, field: String)(implicit
      decoder: Decoder[T]
  ) = {
    json.hcursor.downField("data").get[T](field) match {
      case Right(value)  => value
      case Left(failure) => throw new RuntimeException(failure.message)
    }
  }

  private def json(string: String) = {
    parse(string) match {
      case Right(variables) => variables
      case Left(failure)    => throw new RuntimeException(failure.message)
    }
  }
}
