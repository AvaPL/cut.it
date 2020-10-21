package integration.tests.common

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ElasticsearchContainer, KafkaContainer}
import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import link.store.config.{BulkConfig, ElasticConfig}
import link.store.elasticsearch.ElasticConnector
import links.kafka.{KafkaConfig, KafkaConnector}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

trait IntegrationTest
    extends AnyWordSpec
    with Matchers
    with TestContainersForAll
    with ScalatestRouteTest {
  override type Containers = KafkaContainer and ElasticsearchContainer

  override def startContainers(): Containers = {
    val kafka = KafkaContainer.Def().start()
    val elasticsearch =
      ElasticsearchContainer.Def("bitnami/elasticsearch").start()
    kafka and elasticsearch
  }

  def testKafkaConnector(kafka: KafkaContainer): KafkaConnector = {
    val kafkaConfig = KafkaConfig(kafka.bootstrapServers)
    KafkaConnector(kafkaConfig)
  }

  def testElasticConnector(
      elasticsearch: ElasticsearchContainer
  ): ElasticConnector = {
    val bulkConfig    = BulkConfig(1000, 10.seconds)
    val elasticConfig = ElasticConfig(elasticsearch.httpHostAddress, bulkConfig)
    ElasticConnector(elasticConfig)
  }
}
