package graphql.schema

import java.time.format.DateTimeFormatter

import graphql.model.Link
import graphql.service.LinkService
import io.circe.generic.auto._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema._

object SchemaDefinition {
  val QueryType: ObjectType[Unit, Unit] =
    ObjectType(
      "Query",
      fields[Unit, Unit](
        // Dummy query because GraphQL spec enforces it
        Field(
          "isAlive",
          BooleanType,
          description = Some("Checks if service is alive"),
          resolve = _ => true
        )
      )
    )
  val LinkType: ObjectType[Unit, Link] = deriveObjectType[Unit, Link](
    ReplaceField(
      "created",
      Field(
        "created",
        StringType,
        resolve = _.value.created.format(DateTimeFormatter.RFC_1123_DATE_TIME)
      )
    )
  )
  val UriArgument: Argument[String] = Argument("uri", StringType)
  val MutationType: ObjectType[LinkService, Unit] = ObjectType(
    "Mutation",
    fields[LinkService, Unit](
      Field(
        "cutLink",
        LinkType,
        description = Some("Cuts a link and returns it."),
        arguments = UriArgument :: Nil,
        resolve = c => c.ctx.cutLink(c.arg(UriArgument))
      )
    )
  )
  val schema: Schema[LinkService, Unit] =
    Schema(QueryType, Some(MutationType))
}
