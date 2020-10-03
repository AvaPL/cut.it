package graphql.schema

import java.time.format.DateTimeFormatter

import graphql.model.Link
import graphql.service.LinkService
import io.circe.generic.auto._
import sangria.macros.derive._
import sangria.marshalling.circe._
import sangria.schema._

import scala.concurrent.Await
import scala.concurrent.duration._

object SchemaDefinition {
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
  val IdArgument: Argument[String] = Argument("id", IDType)
  val QueryType: ObjectType[LinkService, Unit] = ObjectType(
    "Query",
    fields[LinkService, Unit](
      Field(
        "uncutLink",
        OptionType(LinkType),
        description = Some("Returns a cut link for specified `id`."),
        arguments = IdArgument :: Nil,
        resolve = c => c.ctx.uncutLink(c.arg(IdArgument))
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
        resolve =
          c => Await.result(c.ctx.cutLink(c.arg(UriArgument)), 10.seconds)
      )
    )
  )

  val schema: Schema[LinkService, Unit] =
    Schema(QueryType, Some(MutationType))
}
