package link.store.elasticsearch

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.stream.scaladsl.{Flow, FlowWithContext}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.akka.{AkkaHttpClient, AkkaHttpClientSettings}
import com.sksamuel.elastic4s.requests.bulk.BulkResponse
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.{ElasticClient, ElasticError, Response}
import link.store.config.{BulkConfig, ElasticConfig}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.{ExecutionContext, Future}

case class ElasticConnector(client: ElasticClient, bulkConfig: BulkConfig)(
    implicit as: ActorSystem
) {
  def bulkIndexConsumerRecordFlow(index: String): Flow[
    (ConsumerRecord[String, String], CommittableOffset),
    (BulkResponse, CommittableOffset),
    NotUsed
  ] =
    Flow[(ConsumerRecord[String, String], CommittableOffset)]
      .groupedWithin(bulkConfig.maxElements, bulkConfig.timeWindow)
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
        sys.error(s"Request failed: $error")
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

  def getDocument(index: String, id: String): Future[String] = {
    implicit val ec: ExecutionContext = as.dispatcher
    client
      .execute {
        get(index, id)
      }
      .map(_.toEither)
      .map(extractDocument)
  }

  private def extractDocument(response: Either[ElasticError, GetResponse]) =
    response match {
      case Right(response) if response.found => response.sourceAsString
      case Right(response) =>
        throw new NoSuchElementException(
          s"Element with id ${response.id} not found in index ${response.index}"
        )
      case Left(error) => throw error.asException
    }
}

object ElasticConnector {
  def apply(
      config: ElasticConfig
  )(implicit as: ActorSystem): ElasticConnector = {
    val hosts              = config.hosts.split(',').toSeq
    val httpClientSettings = AkkaHttpClientSettings(hosts)
    val httpClient         = AkkaHttpClient(httpClientSettings)
    val elasticClient      = ElasticClient(httpClient)
    ElasticConnector(elasticClient, config.bulk)
  }
}
