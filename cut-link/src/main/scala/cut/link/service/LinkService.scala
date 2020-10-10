package cut.link.service

import java.util.Base64

import akka.actor.ActorSystem
import akka.stream.{OverflowStrategy, QueueOfferResult}
import akka.stream.QueueOfferResult.Enqueued
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import cut.link.model.Link
import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.kafka.clients.producer.ProducerRecord

import scala.util.{Failure, Success, Try}

case class LinkService(producerSink: Sink[ProducerRecord[String, String], _])(
    implicit as: ActorSystem
) {
  val cutLinkFlow: SourceQueueWithComplete[Link] = Source
    .queue[Link](1000, OverflowStrategy.dropNew)
    .map(linkProducerRecord)
    .via(logRecord)
    .to(producerSink)
    .run()
  val cutLinkTopic          = "cut_link"
  private val base64Encoder = Base64.getUrlEncoder

  def cutLink(uri: String): Link = {
    val id   = createLinkId(uri)
    val link = Link(id, uri)
    scribe.info(s"Created a link: $link")
    sendLinkMessage(link)
    link
  }

  private def createLinkId(uri: String) = {
    val hashcode      = uri.hashCode
    val hashcodeBytes = BigInt(hashcode).toByteArray
    // Drops '=' chars which are the result of base64 padding
    base64Encoder.encodeToString(hashcodeBytes).reverse.dropWhile(_ == '=')
  }

  private def sendLinkMessage(link: Link): Unit = {
    val enqueueResult = cutLinkFlow.offer(link)
    enqueueResult.onComplete(logEnqueueResult)(as.dispatcher)
  }

  private def logEnqueueResult(result: Try[QueueOfferResult]): Unit =
    result match {
      case Success(status) if status == Enqueued =>
        scribe.debug(s"Successfully enqueued Kafka message")
      case Success(status) =>
        scribe.error(s"Failed to enqueue Kafka message: $status")
      case Failure(exception) =>
        scribe.error(s"Failed to enqueue Kafka message: $exception")
    }

  private def linkProducerRecord(link: Link) =
    new ProducerRecord(cutLinkTopic, link.id, link.asJson.noSpaces)

  private def logRecord = Flow[ProducerRecord[String, String]].map { record =>
    scribe.debug(s"Sending link to Kafka: ${record.value}")
    record
  }
}
