package graphql.schema

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
  val schema: Schema[LinkService, Unit] = SchemaDefinition.schema
  val linkService: LinkService          = LinkService()

  "SchemaDefinition" when {
    "queried for uncut link" should {
      "return a link with specified id" in {
        val uncutLinkQuery =
          graphql"""
            query($$id: ID!) {
              uncutLink(id: $$id) {
                id,
                uri,
                created
              }
            }
          """
        val id        = "123abc"
        val variables = idVariables(id)

        val future = query(uncutLinkQuery, variables)
          .map(parseData[Link](_, "uncutLink"))
        val link = Await.result(future, 10.seconds)

        link.id should be(id)
      }

      "return null for nonexistent id" in {
        val uncutLinkQuery =
          graphql"""
            query($$id: ID!) {
              uncutLink(id: $$id) {
                id,
                uri,
                created
              }
            }
          """
        val id        = "error"
        val variables = idVariables(id)

        val future = query(uncutLinkQuery, variables)
          .map(parseData[Option[Link]](_, "uncutLink"))
        val link = Await.result(future, 10.seconds)

        link should be(None)
      }
    }

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

  private def idVariables(id: String) =
    json(s"""|{
             |  "id": "$id"
             |}
             |""".stripMargin)

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
