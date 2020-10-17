package links.model

import java.time.OffsetDateTime

import sangria.macros.derive.GraphQLOutputType
import sangria.schema.IDType

case class Link(
    @GraphQLOutputType(IDType) id: String,
    uri: String,
    created: OffsetDateTime
)

object Link {
  def apply(
      id: String,
      uri: String,
      created: OffsetDateTime = OffsetDateTime.now()
  ): Link = new Link(id, uri, created)
}
