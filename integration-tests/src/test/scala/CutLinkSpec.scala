import com.dimafeng.testcontainers.lifecycle._
import com.dimafeng.testcontainers.{ElasticsearchContainer, KafkaContainer}
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CutLinkSpec extends AnyWordSpec with Matchers with TestContainersForAll {
  override type Containers = KafkaContainer and ElasticsearchContainer

  override def startContainers(): Containers = {
    val kafka         = KafkaContainer.Def().start()
    val elasticsearch = ElasticsearchContainer.Def().start()
    kafka and elasticsearch
  }
}
