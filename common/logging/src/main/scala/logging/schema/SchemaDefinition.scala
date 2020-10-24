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
  private val packageField            = "package"
  private val packageFieldDescription = "Logging level for service package"
  private val librariesField          = "libraries"
  private val librariesFieldDescription =
    "Logging level for libraries outside service package"

  val LevelsType: ObjectType[Unit, Levels] = ObjectType(
    "Levels",
    fields[Unit, Levels](
      Field(
        packageField,
        StringType,
        description = Some(packageFieldDescription),
        resolve = _.value.`package`.name
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
          packageField,
          OptionInputType(StringType),
          packageFieldDescription
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
        val fields    = node.asInstanceOf[Map[String, Any]]
        val `package` = fields.get(packageField).map(_.toString)
        val libraries = fields.get(librariesField).map(_.toString)
        LevelsChange(`package`, libraries)
      }
    }
  val LevelsArgument: Argument[LevelsChange] =
    Argument("levelsChangeInput", LevelsChangeInputType)
  val MutationType: ObjectType[LoggingService, Unit] = ObjectType(
    "Mutation",
    fields[LoggingService, Unit](
      Field(
        "changeLevels",
        LevelsType,
        arguments = LevelsArgument :: Nil,
        resolve = c => c.ctx.setMinimumLoggingLevels(c.arg(LevelsArgument))
      )
    )
  )

  val schema: Schema[LoggingService, Unit] =
    Schema(QueryType, Some(MutationType))
}
