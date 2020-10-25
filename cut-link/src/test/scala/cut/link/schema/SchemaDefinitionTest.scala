package cut.link.schema

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import cut.link.flow.LinkMessageFlow
import cut.link.service.LinkService
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.{Decoder, Json}
import kafka.KafkaConnector
import links.model.Link
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sangria.ast.Document
import sangria.execution.Executor
import sangria.macros._
import sangria.marshalling.circe._
import sangria.schema.Schema

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SchemaDefinitionTest extends AnyWordSpec with Matchers with MockFactory {
  implicit val system: ActorSystem = ActorSystem("test")

  val schema: Schema[LinkService, Unit]  = SchemaDefinition.schema
  val mockKafkaConnector: KafkaConnector = mock[KafkaConnector]
  (mockKafkaConnector
    .producer(_: ActorSystem))
    .expects(*)
    .returning(Sink.ignore)
  val ignoreFlow: LinkMessageFlow = LinkMessageFlow(mockKafkaConnector)
  val linkService: LinkService    = LinkService(ignoreFlow)

  "SchemaDefinition" when {
    "received cut link mutation" should {
      "cut link" in {
        val uri = "http://test.com"

        val link = cutLink(uri)

        link.uri should be(uri)
      }
    }
  }

  private def cutLink(uri: String) = {
    val queryFuture = query(cutLinkMutation, uriVariables(uri))
      .map(parseData[Link](_, "cutLink"))
    Await.result(queryFuture, 10.seconds)
  }

  private def query(userQuery: Document, variables: Json) =
    Executor
      .execute(
        schema = schema,
        queryAst = userQuery,
        variables = variables,
        userContext = linkService
      )

  private def cutLinkMutation =
    graphql"""
            mutation($$uri: String!) {
              cutLink(uri: $$uri) {
                id,
                uri,
                created
              }
            }
          """

  private def uriVariables(uri: String) =
    json(s"""|{
             |  "uri": "$uri"
             |}
             |""".stripMargin)

  private def json(string: String) =
    parse(string) match {
      case Right(variables) => variables
      case Left(failure)    => throw failure
    }

  private def parseData[T](json: Json, field: String)(implicit
      decoder: Decoder[T]
  ) =
    json.hcursor.downField("data").get[T](field) match {
      case Right(value)  => value
      case Left(failure) => throw new RuntimeException(failure.message)
    }
}
