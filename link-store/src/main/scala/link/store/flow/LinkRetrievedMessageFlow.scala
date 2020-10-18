package link.store.flow

import akka.actor.ActorSystem
import akka.stream.QueueOfferResult.Enqueued
import akka.stream.scaladsl.{Flow, Source, SourceQueueWithComplete}
import akka.stream.{OverflowStrategy, QueueOfferResult}
import io.circe.generic.auto._
import io.circe.syntax._
import links.kafka.{KafkaConnector, Topic}
import links.model.Link
import org.apache.kafka.clients.producer.ProducerRecord

import scala.util.{Failure, Success, Try}

case class LinkRetrievedMessageFlow(kafkaConnector: KafkaConnector)(implicit
    as: ActorSystem
) {
  private val retrievedLinkSink = kafkaConnector.producer
  private val retrievedLinkQueue: SourceQueueWithComplete[Link] = Source
    .queue[Link](1000, OverflowStrategy.dropNew)
    .map(linkProducerRecord)
    .via(logRecord)
    .to(retrievedLinkSink)
    .run()

  def sendLinkRetrievedMessage(link: Link): Unit = {
    val enqueueResult = retrievedLinkQueue.offer(link)
    enqueueResult.onComplete(logEnqueueResult)(as.dispatcher)
  }

  private def logEnqueueResult(result: Try[QueueOfferResult]): Unit =
    result match {
      case Success(status) if status == Enqueued =>
        scribe.trace(s"Successfully enqueued Kafka message")
      case Success(status) =>
        scribe.error(s"Failed to enqueue Kafka message: $status")
      case Failure(exception) =>
        scribe.error(s"Failed to enqueue Kafka message: $exception")
    }

  private def linkProducerRecord(link: Link) =
    new ProducerRecord(Topic.retrievedLinkTopic, link.id, link.asJson.noSpaces)

  private def logRecord = Flow[ProducerRecord[String, String]].map { record =>
    scribe.debug(s"Sending retrieved link to Kafka: ${record.value}")
    record
  }
}
