package schema

import io.circe.Decoder.Result
import io.circe.Json
import model.User
import repository.UserRepository
import sangria.schema._
import sangria.macros.derive._
import io.circe.generic.auto._
import sangria.marshalling.circe._

object SchemaDefinition {
  val UserType: ObjectType[Unit, User] = deriveObjectType[Unit, User]()
  val UsernameArgument: Argument[String] = Argument("username", StringType)
  val QueryType: ObjectType[UserRepository, Unit] = ObjectType(
    "Query",
    fields[UserRepository, Unit](
      Field(
        "user",
        OptionType(UserType),
        description = Some("Returns a user with specified `username`."),
        arguments = UsernameArgument :: Nil,
        resolve = c => c.ctx.user(c.arg(UsernameArgument))
      ),
      Field(
        "users",
        ListType(UserType),
        description = Some("Returns a list of all users"),
        resolve = _.ctx.users
      )
    )
  )

  val UserInputType: InputObjectType[User] = deriveInputObjectType[User](
    InputObjectTypeName("UserInput")
  )
  implicit val userFromInput: Json => Result[User] = _.as[User]
  val UserArgument: Argument[User] = Argument("userInput", UserInputType)
  val MutationType: ObjectType[UserRepository, Unit] = ObjectType(
    "Mutation",
    fields[UserRepository, Unit](
      Field(
        "createUser",
        UserType,
        arguments = UserArgument :: Nil,
        resolve = c => c.ctx.addUser(c.arg(UserArgument))
      ),
      Field(
        "deleteUser",
        OptionType(UserType),
        arguments = UsernameArgument :: Nil,
        resolve = c => c.ctx.deleteUser(c.arg(UsernameArgument))
      )
    )
  )

  val schema: Schema[UserRepository, Unit] =
    Schema(QueryType, Some(MutationType))
}
