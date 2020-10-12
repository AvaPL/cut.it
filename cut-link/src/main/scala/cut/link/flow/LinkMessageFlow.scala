package cut.link.flow

import akka.actor.ActorSystem
import akka.stream.QueueOfferResult.Enqueued
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import akka.stream.{OverflowStrategy, QueueOfferResult}
import cut.link.model.Link
import io.circe.generic.auto._
import io.circe.syntax._
import links.kafka.Topic
import org.apache.kafka.clients.producer.ProducerRecord

import scala.util.{Failure, Success, Try}

case class LinkMessageFlow(linkSink: Sink[ProducerRecord[String, String], _])(
    implicit as: ActorSystem
) {
  val cutLinkFlow: SourceQueueWithComplete[Link] = Source
    .queue[Link](1000, OverflowStrategy.dropNew)
    .map(linkProducerRecord)
    .via(logRecord)
    .to(linkSink)
    .run()

  def sendLinkMessage(link: Link): Unit = {
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
    new ProducerRecord(Topic.cutLinkTopic, link.id, link.asJson.noSpaces)

  private def logRecord = Flow[ProducerRecord[String, String]].map { record =>
    scribe.debug(s"Sending link to Kafka: ${record.value}")
    record
  }
}
