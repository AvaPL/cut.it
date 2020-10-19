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
      "send 10 elements for 10 elements bulk limit and 15 elements sent" in withElasticConnector {
        elasticConnector =>
          val index     = randomAscii
          val indexFlow = elasticConnector.bulkIndexConsumerRecordFlow(index)
          val recordsSource = Source.queue[
            (ConsumerRecord[String, String], ConsumerMessage.CommittableOffset)
          ](1000, OverflowStrategy.dropNew)

          val (queue, firstBulkComplete) =
            recordsSource.via(indexFlow).toMat(Sink.head)(Keep.both).run()
          List.fill(15)(testRecord).foreach(queue.offer)
          Await.ready(firstBulkComplete, 10.seconds)
          Thread.sleep(1000) // Additional second to index records
          val countFuture = elasticConnector.client.execute {
            count(index)
          }
          val countResponse = Await.result(countFuture, 10.seconds)
          val countValue    = countResponse.map(_.count).result

          countValue should be(10)
      }(BulkConfig(10, 1.minute))
    }

    // TODO: Add test with timeWindow
    // TODO: Cleanup ALL tests (including new and old ones), simplify by extracting methods
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

  private def withElasticConnector[T](
      runTest: ElasticConnector => T
  )(implicit bulkConfig: BulkConfig): T =
    withContainers { container =>
      val elasticConfig    = ElasticConfig(container.httpHostAddress, bulkConfig)
      val elasticConnector = ElasticConnector(elasticConfig)
      runTest(elasticConnector)
    }

  private def randomAscii = Random.alphanumeric.take(10).mkString.toLowerCase
}
