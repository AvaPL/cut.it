package graphql.service

import java.util.Base64

import akka.Done
import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import graphql.model.Link
import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.Future

case class LinkService(
    producerSink: Sink[ProducerRecord[String, String], Future[Done]]
)(implicit as: ActorSystem) {

  val cutLinkFlow: SourceQueueWithComplete[Link] = Source
    .queue[Link](1000, OverflowStrategy.dropNew)
    .map(linkProducerRecord)
    .via(logRecord)
    .to(producerSink)
    .run()

  val cutLinkTopic = "cut_link"

  private val base64Encoder = Base64.getUrlEncoder

  def cutLink(uri: String): Link = {
    val id   = createLinkId(uri)
    val link = Link(id, uri)
    cutLinkFlow.offer(link) // TODO: Check enqueue result
    link
  }

  private def createLinkId(uri: String) = {
    val hashcode      = uri.hashCode
    val hashcodeBytes = BigInt(hashcode).toByteArray
    base64Encoder.encodeToString(hashcodeBytes)
  }

  private def linkProducerRecord(link: Link) =
    new ProducerRecord(cutLinkTopic, link.id, link.asJson.noSpaces)

  private def logRecord = Flow[ProducerRecord[String, String]].map { record =>
    scribe.info(s"Created a link: ${record.value}")
    record
  }
}
