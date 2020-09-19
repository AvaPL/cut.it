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
  val userRepository: UserRepository       = UserRepository()

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
          .map(parseData[List[User]](_, "users"))
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
          .map(parseData[Option[User]](_, "user"))
        val user = Await.result(future, 10.seconds)

        user should be(userRepository.user("Pafeu"))
      }

      "return null for nonexistent user" in {
        val variables = usernameVariables("Nonexistent")

        val future = query(userQuery, variables)
          .map(parseData[Option[User]](_, "user"))
        val user = Await.result(future, 10.seconds)

        user should be(None)
      }
    }

    "received users mutation" should {

      val createUserMutation =
        graphql"""
            mutation($$userInput: UserInput!) {
              createUser(userInput: $$userInput) {
                username,
                status
              }
            }
          """

      "create user" in {
        val newUser   = User("Adrian", "New here")
        val variables = userVariables(newUser)

        val future = query(createUserMutation, variables)
          .map(parseData[User](_, "createUser"))
        val user = Await.result(future, 10.seconds)

        user should be(newUser)
        userRepository.users should contain(newUser)
      }

      val deleteUserMutation =
        graphql"""
            mutation($$username: String!) {
              deleteUser(username: $$username) {
                username,
                status
              }
            }
          """

      "remove user" in {
        val predefinedUser = User("Mario", "Chillin'")
        userRepository.addUser(predefinedUser)
        val variables = usernameVariables(predefinedUser.username)

        val future = query(deleteUserMutation, variables)
          .map(parseData[Option[User]](_, "deleteUser"))
        val user = Await.result(future, 10.seconds)

        user.map(_.username) should be(Some(predefinedUser.username))
        userRepository.user(predefinedUser.username) should be(None)
      }

      "return null for nonexistent user" in {
        val usersBefore = userRepository.users
        val variables   = usernameVariables("Nonexistent")

        val future = query(deleteUserMutation, variables)
          .map(parseData[Option[User]](_, "deleteUser"))
        val user = Await.result(future, 10.seconds)

        user should be(None)
        userRepository.users should be(usersBefore)
      }
    }
  }

  private def query(userQuery: Document, variables: Json = Json.obj()) =
    Executor
      .execute(
        schema = schema,
        queryAst = userQuery,
        variables = variables,
        userContext = userRepository
      )

  private def parseData[T](json: Json, field: String)(implicit
      decoder: Decoder[T]
  ) =
    json.hcursor.downField("data").get[T](field) match {
      case Right(value)  => value
      case Left(failure) => throw new RuntimeException(failure.message)
    }

  private def usernameVariables(username: String) =
    json(s"""|{
             |  "username": "$username"
             |}
             |""".stripMargin)

  private def json(string: String) =
    parse(string) match {
      case Right(variables) => variables
      case Left(failure)    => throw new RuntimeException(failure.message)
    }

  private def userVariables(user: User) =
    json(s"""|{
             | "userInput": {
             |   "username": "${user.username}",
             |   "status": "${user.status}"
             | }
             |}
             |""".stripMargin)
}
