package logging.schema

import io.circe.generic.auto._
import logging.model.levels.{Levels, LevelsChange}
import logging.service.LoggingService
import sangria.marshalling.circe._
import sangria.marshalling.{
  CoercedScalaResultMarshaller,
  FromInput,
  ResultMarshaller
}
import sangria.schema._

object SchemaDefinition {
  private val serviceField            = "service"
  private val serviceFieldDescription = "Logging level for the service package"
  private val librariesField          = "libraries"
  private val librariesFieldDescription =
    "Logging level for libraries outside the service package"

  val LevelsType: ObjectType[Unit, Levels] = ObjectType(
    "Levels",
    fields[Unit, Levels](
      Field(
        serviceField,
        StringType,
        description = Some(serviceFieldDescription),
        resolve = _.value.service.name
      ),
      Field(
        librariesField,
        StringType,
        description = Some(librariesFieldDescription),
        resolve = _.value.libraries.name
      )
    )
  )
  val QueryType: ObjectType[LoggingService, Unit] =
    ObjectType(
      "Query",
      fields[LoggingService, Unit](
        Field(
          "levels",
          LevelsType,
          description = Some("Returns current logging levels"),
          resolve = _.ctx.currentLevels
        )
      )
    )

  val LevelsChangeInputType: InputObjectType[LevelsChange] =
    InputObjectType[LevelsChange](
      "LevelsChangeInput",
      List(
        InputField(
          serviceField,
          OptionInputType(StringType),
          serviceFieldDescription
        ),
        InputField(
          librariesField,
          OptionInputType(StringType),
          librariesFieldDescription
        )
      )
    )
  implicit val levelsChangeFromInput: FromInput[LevelsChange] =
    new FromInput[LevelsChange] {
      override val marshaller: ResultMarshaller =
        CoercedScalaResultMarshaller.default

      override def fromResult(node: marshaller.Node): LevelsChange = {
        val fields    = node.asInstanceOf[Map[String, Option[String]]]
        val service   = fields.get(serviceField).flatten
        val libraries = fields.get(librariesField).flatten
        LevelsChange.fromStrings(service, libraries)
      }
    }
  val LevelsArgument: Argument[LevelsChange] =
    Argument("levelsChangeInput", LevelsChangeInputType)
  val MutationType: ObjectType[LoggingService, Unit] = ObjectType(
    "Mutation",
    "Changes current logging levels",
    fields[LoggingService, Unit](
      Field(
        "changeLevels",
        LevelsType,
        arguments = LevelsArgument :: Nil,
        resolve = c => c.ctx.changeLevels(c.arg(LevelsArgument))
      )
    )
  )

  val schema: Schema[LoggingService, Unit] =
    Schema(QueryType, Some(MutationType))
}
