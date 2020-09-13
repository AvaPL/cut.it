package schema

import model.User
import repository.UserRepository
import sangria.schema._
import sangria.macros.derive._

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

  val schema: Schema[UserRepository, Unit] = Schema(QueryType)
}
