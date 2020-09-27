package graphql.service

import java.time.OffsetDateTime
import java.util.UUID

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
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser.decode

import scala.util.Random

case class LinkService(config: Config)(implicit as: ActorSystem) {

  val cutLinkInputFlow: SourceQueueWithComplete[Link] = Source
    .queue[Link](1000, OverflowStrategy.dropNew)
    .map { link =>
      // TODO: Add processing logic
      new ProducerRecord(
        "some-input-topic", // TODO: Assign correct topic
        link.id,
        link.asJson.noSpaces
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
      Subscriptions.topics("some-topic") // TODO: Assign correct topic
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
