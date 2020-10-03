package graphql.schema

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import graphql.model.Link
import graphql.service.LinkService
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.{Decoder, Json}
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

class SchemaDefinitionTest extends AnyWordSpec with Matchers {
  implicit val system: ActorSystem = ActorSystem("test")

  val schema: Schema[LinkService, Unit] = SchemaDefinition.schema
  val linkService: LinkService          = LinkService(Sink.ignore)

  "SchemaDefinition" when {
    "received cut link mutation" should {
      "cut link" in {
        val cutLinkMutation =
          graphql"""
            mutation($$uri: String!) {
              cutLink(uri: $$uri) {
                id,
                uri,
                created
              }
            }
          """
        val uri       = "http://test.com"
        val variables = uriVariables(uri)

        val future = query(cutLinkMutation, variables)
          .map(parseData[Link](_, "cutLink"))
        val link = Await.result(future, 10.seconds)

        link.uri should be(uri)
      }
    }
  }

  private def query(userQuery: Document, variables: Json) =
    Executor
      .execute(
        schema = schema,
        queryAst = userQuery,
        variables = variables,
        userContext = linkService
      )

  private def parseData[T](json: Json, field: String)(implicit
      decoder: Decoder[T]
  ) =
    json.hcursor.downField("data").get[T](field) match {
      case Right(value)  => value
      case Left(failure) => throw new RuntimeException(failure.message)
    }

  private def json(string: String) =
    parse(string) match {
      case Right(variables) => variables
      case Left(failure)    => throw failure
    }

  private def uriVariables(uri: String) =
    json(s"""|{
             |  "uri": "$uri"
             |}
             |""".stripMargin)
}
