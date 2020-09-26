package graphql.model

import java.time.OffsetDateTime

import sangria.macros.derive.GraphQLOutputType
import sangria.schema.IDType

case class Link(
    @GraphQLOutputType(IDType) id: String,
    uri: String,
    created: OffsetDateTime
)
