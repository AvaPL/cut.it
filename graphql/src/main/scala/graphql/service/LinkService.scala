package graphql.service

import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer, Producer}
import akka.kafka.{
  CommitterSettings,
  ConsumerSettings,
  ProducerSettings,
  Subscriptions
}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import com.typesafe.config.Config
import graphql.model.Link
import io.circe.generic.auto._
import io.circe.parser.decode
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}

import scala.concurrent.{Future, Promise}
import scala.util.Random

case class LinkService(config: Config)(implicit as: ActorSystem) {

  private val uriToLinkPromise = new ConcurrentHashMap[String, Promise[Link]]()

  val cutLinkInputFlow: SourceQueueWithComplete[String] = Source
    .queue[String](1000, OverflowStrategy.dropNew)
    .map { uri =>
      new ProducerRecord(
        "some-input-topic", // TODO: Assign correct topic
        uri,
        uri
      )
    }
    .map { record =>
      println(record) // TODO: Replace with logging
      record
    }
    .to(
      Producer.plainSink(
        ProducerSettings(config, new StringSerializer, new StringSerializer)
      )
    )
    .run()

  val cutLinkOutputFlow: Consumer.Control = Consumer
    .committableSource(
      ConsumerSettings(config, new StringDeserializer, new StringDeserializer),
      Subscriptions.topics("some-output-topic") // TODO: Assign correct topic
    )
    .asSourceWithContext(_.committableOffset)
    .map(_.record)
    .map { record =>
      println(record) // TODO: Replace with logging
      val id   = record.key
      val link = decode[Link](record.value)
      println(s"id: $id, link: $link") // TODO: Add processing logic
      record
    }
    .asSource
    .map(_._2)
    .toMat(Committer.sink(CommitterSettings(config)))(DrainingControl.apply)
    .run()

  def cutLink(uri: String): Future[Link] = {
    val promise = Promise[Link]()
    uriToLinkPromise.put(uri, promise) // TODO: Use flow instead of map
    cutLinkInputFlow.offer(uri)        // TODO: Check enqueue result
    promise.future
  }

  def uncutLink(id: String): Option[Link] = {
    // TODO: Replace with sending a request message to Kafka
    val now     = OffsetDateTime.now()
    val created = now.minusSeconds(Random.between(0, now.toEpochSecond))
    Option.when(id != "error")(Link(id, "http://mock.com", created))
  }
}
