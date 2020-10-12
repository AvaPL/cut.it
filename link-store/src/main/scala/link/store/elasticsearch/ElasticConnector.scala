package link.store.elasticsearch

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.stream.scaladsl.{Flow, FlowWithContext}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.akka.{AkkaHttpClient, AkkaHttpClientSettings}
import com.sksamuel.elastic4s.requests.bulk.BulkResponse
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.{ElasticClient, Response}
import link.store.config.ElasticConfig
import org.apache.kafka.clients.consumer.ConsumerRecord

case class ElasticConnector(config: ElasticConfig)(implicit as: ActorSystem) {
  val client: ElasticClient = ElasticClient(
    AkkaHttpClient(AkkaHttpClientSettings(hosts = config.hostsSeq))
  )

  def bulkIndexConsumerRecordFlow(index: String): Flow[
    (ConsumerRecord[String, String], CommittableOffset),
    (BulkResponse, CommittableOffset),
    NotUsed
  ] =
    Flow[(ConsumerRecord[String, String], CommittableOffset)]
      .groupedWithin(config.bulk.maxElements, config.bulk.timeWindow)
      .map(toIndexRequests(index))
      .via(bulkIndexFlow)

  private def toIndexRequests(index: String)(
      recordsOffsets: Seq[(ConsumerRecord[String, String], CommittableOffset)]
  ) = recordsOffsets.unzip match {
    case (records, offsets) =>
      val documents     = records.map(_.value)
      val indexRequests = documents.map(indexInto(index).doc(_))
      val lastOffset    = offsets.last
      (indexRequests, lastOffset)
  }

  private def bulkIndexFlow =
    FlowWithContext[Seq[IndexRequest], CommittableOffset]
      .mapAsync(1) { indexRequests =>
        client.execute {
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
        scribe.info(s"Saved consumer records to Elasticsearch: $result")
        result
    }
}
