package graphql.service

import java.time.OffsetDateTime
import java.util.UUID

import graphql.model.Link

import scala.util.Random

case class LinkService() {
  def cutLink(uri: String): Link = {
    // TODO: Replace with sending a request message to Kafka
    val created = OffsetDateTime.now()
    Link(UUID.randomUUID().toString, uri, created)
  }

  def uncutLink(id: String): Option[Link] = {
    // TODO: Replace with sending a request message to Kafka
    val now     = OffsetDateTime.now()
    val created = now.minusSeconds(Random.between(0, now.toEpochSecond))
    Option.when(id != "error")(Link(id, "http://mock.com", created))
  }
}
