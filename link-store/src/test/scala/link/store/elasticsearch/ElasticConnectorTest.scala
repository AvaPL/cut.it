package link.store.elasticsearch

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage
import akka.kafka.testkit.ConsumerResultFactory
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.dimafeng.testcontainers.ElasticsearchContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.sksamuel.elastic4s.ElasticDsl._
import link.store.config.{BulkConfig, ElasticConfig}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Random}

class ElasticConnectorTest
    extends AnyWordSpec
    with Matchers
    with TestContainerForAll {
  implicit val system: ActorSystem           = ActorSystem("test")
  implicit val defaultBulkConfig: BulkConfig = BulkConfig(1000, 10.seconds)

  override val containerDef: ElasticsearchContainer.Def =
    ElasticsearchContainer.Def("bitnami/elasticsearch")

  "ElasticConnector" when {
    "retrieving a document" should {
      "return an existing document" in withElasticConnector {
        elasticConnector =>
          val index        = randomAscii
          val id           = randomAscii
          val testDocument = """{"key":"value"}"""
          val indexFuture = elasticConnector.client.execute {
            indexInto(index).withId(id).doc(testDocument)
          }
          Await.ready(indexFuture, 10.seconds)

          val documentFuture = elasticConnector.getDocument(index, id)
          val document       = Await.result(documentFuture, 10.seconds)

          document should be(testDocument)
      }

      "fail for nonexistent document" in withElasticConnector {
        elasticConnector =>
          val documentFuture =
            elasticConnector.getDocument("nonexistentIndex", "id")
          Await.ready(documentFuture, 10.seconds)

          documentFuture.value.get shouldBe a[Failure[_]]
      }
    }

    "indexing consumer records in bulk" should {
      "send 10 elements for 10 elements bulk limit, 1 minute time window and 15 elements sent" in withElasticConnector {
        elasticConnector =>
          val index = randomAscii

          bulkIndexTestRecords(elasticConnector, index, 15)
          val count = countDocuments(elasticConnector, index)

          count should be(10)
      }(BulkConfig(10, 1.minute))

      "send 1 element after 2 seconds for 10 elements bulk limit, 2 seconds time window and 1 element sent" in withElasticConnector {
        elasticConnector =>
          val index = randomAscii

          bulkIndexTestRecords(elasticConnector, index, 1)
          val count = countDocuments(elasticConnector, index)

          count should be(1)
      }(BulkConfig(10, 2.seconds))
    }

    // TODO: Cleanup ALL tests (including new and old ones), simplify by extracting methods
  }

  private def withElasticConnector[T](
      runTest: ElasticConnector => T
  )(implicit bulkConfig: BulkConfig): T =
    withContainers { container =>
      val elasticConfig    = ElasticConfig(container.httpHostAddress, bulkConfig)
      val elasticConnector = ElasticConnector(elasticConfig)
      runTest(elasticConnector)
    }

  private def randomAscii = Random.alphanumeric.take(10).mkString.toLowerCase

  private def bulkIndexTestRecords(
      elasticConnector: ElasticConnector,
      index: String,
      count: Int
  ) = {
    val indexFlow = elasticConnector.bulkIndexConsumerRecordFlow(index)
    val recordsSource = Source.queue[
      (ConsumerRecord[String, String], ConsumerMessage.CommittableOffset)
    ](1000, OverflowStrategy.dropNew)
    val (queue, firstBulkComplete) =
      recordsSource.via(indexFlow).toMat(Sink.head)(Keep.both).run()
    List.fill(count)(testRecord).foreach(queue.offer)
    Await.ready(firstBulkComplete, 10.seconds)
    waitForRefresh(elasticConnector)
  }

  private def testRecord =
    (
      new ConsumerRecord(
        "testTopic",
        0,
        0,
        randomAscii,
        """{"key":"value"}"""
      ),
      ConsumerResultFactory.committableOffset(
        "groupId",
        "testTopic",
        0,
        0,
        ""
      )
    )

  private def waitForRefresh(elasticConnector: ElasticConnector) = {
    val refreshFuture = elasticConnector.client.execute {
      indexInto(randomAscii).refreshImmediately
    }
    Await.ready(refreshFuture, 10.seconds)
  }

  private def countDocuments(
      elasticConnector: ElasticConnector,
      index: String
  ) = {
    val countFuture = elasticConnector.client.execute {
      count(index)
    }
    val countResponse = Await.result(countFuture, 10.seconds)
    countResponse.map(_.count).result
  }
}
