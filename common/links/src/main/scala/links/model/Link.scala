package links.model

import java.time.OffsetDateTime

import sangria.macros.derive.GraphQLOutputType
import sangria.schema.IDType

/** Represents a link that can be stored.
  * @param id id that is used for link retrieval from storage
  * @param uri uri
  * @param created creation date and time
  */
case class Link(
    @GraphQLOutputType(IDType) id: String,
    uri: String,
    created: OffsetDateTime
)

object Link {

  /** Creates a link with creation time set to object creation time.
    */
  def apply(id: String, uri: String): Link =
    Link(id, uri, OffsetDateTime.now())
}
