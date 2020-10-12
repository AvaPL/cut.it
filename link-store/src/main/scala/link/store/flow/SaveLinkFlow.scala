package link.store.flow

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.CommitterSettings
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.Committer
import akka.stream.scaladsl.{Flow, FlowWithContext, SourceWithContext}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.akka.{AkkaHttpClient, AkkaHttpClientSettings}
import com.sksamuel.elastic4s.requests.bulk.BulkResponse
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.{ElasticClient, Response}
import links.kafka.Topic
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.Future
import scala.concurrent.duration._

case class SaveLinkFlow(
    consumerSource: String => SourceWithContext[
      ConsumerRecord[String, String],
      CommittableOffset,
      _
    ]
)(implicit as: ActorSystem) {
  // TODO:
  //  Take message from Kafka             - DONE
  //  Save message value to Elasticsearch
  //  Commit offset to Kafka              - DONE
  //  + add appropriate logging on each step
  val esClient: ElasticClient = ElasticClient( // TODO: Inject client with flow
    AkkaHttpClient(
      AkkaHttpClientSettings(hosts =
        Seq("172.17.0.1:9200")
      ) // TODO: Read hosts from config
    )
  )
  val linkStoreIndex = ""

  val saveLinkFlow: Future[Done] =
    consumerSource(Topic.cutLinkTopic)
      .via(logRecord)
      .via(indexFlow) // TODO: Inject this flow via argument
      .runWith(committerSink)

  private def logRecord =
    Flow[(ConsumerRecord[String, String], CommittableOffset)].map {
      case (record, ctx) =>
        scribe.debug(s"Received link from Kafka: ${record.value}")
        (record, ctx)
    }

  private def indexFlow =
    Flow[(ConsumerRecord[String, String], CommittableOffset)]
      .groupedWithin(1000, 10.seconds)
      .map(toIndexRequests)
      .via(bulkIndexFlow)

  private def toIndexRequests(
      recordsOffsets: Seq[(ConsumerRecord[String, String], CommittableOffset)]
  ) = recordsOffsets.unzip match {
    case (records, offsets) =>
      val documents     = records.map(_.value)
      val indexRequests = documents.map(indexInto(linkStoreIndex).doc(_))
      val lastOffset    = offsets.last
      (indexRequests, lastOffset)
  }

  private def bulkIndexFlow =
    FlowWithContext[Seq[IndexRequest], CommittableOffset]
      .mapAsync(1) { indexRequests =>
        esClient.execute {
          bulk(indexRequests)
        }
      }
      .map(toBulkResponse)

  private def toBulkResponse(response: Response[BulkResponse]) =
    response.toEither match {
      case Left(error) =>
        sys.error(s"Request failed: $error")
      case Right(result) =>
        if (result.hasFailures)
          sys.error(s"Request result contained failures: ${result.failures}")
        result
    }

  private def committerSink =
    Committer.sinkWithOffsetContext(CommitterSettings(as))
}
