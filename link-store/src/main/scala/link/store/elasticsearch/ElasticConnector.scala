package link.store.elasticsearch

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
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
  val client: ElasticClient = {
    val hosts              = config.hosts.split(',').toSeq
    val httpClientSettings = AkkaHttpClientSettings(hosts)
    val httpClient         = AkkaHttpClient(httpClientSettings)
    ElasticClient(httpClient)
  }

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
      val documents = records.map(record => (record.key, record.value))
      val indexRequests = documents.map { case (id, document) =>
        indexInto(index).withId(id).createOnly(true).doc(document)
      }
      val lastOffset = offsets.last
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
        sys.error(
          s"Request failed: $error"
        ) // TODO: Add scribe-slf4j because stream failure is almost invisible in logs
      case Right(result) =>
        if (hasFailures(result))
          sys.error(s"Request result contained failures: ${result.failures}")
        logResponse(result)
        result
    }

  private def hasFailures(result: BulkResponse): Boolean =
    // Conflict means that a link already existed and was not updated
    result.failures.exists(_.status != StatusCodes.Conflict.intValue)

  private def logResponse(response: BulkResponse): Unit = {
    val count          = response.successes.size
    val keys           = response.successes.map(_.id).mkString("[", ", ", "]")
    val durationMillis = response.took
    if (response.hasSuccesses)
      scribe.info(
        s"Saved $count consumer records with keys $keys to Elasticsearch in $durationMillis ms"
      )
    if (response.hasFailures) {
      val skippedCount = response.failures.size
      scribe.debug(s"$skippedCount duplicate consumer records skipped")
    }
  }
}
