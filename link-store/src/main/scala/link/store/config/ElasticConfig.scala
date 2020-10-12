package link.store.config

import scala.concurrent.duration.FiniteDuration

case class ElasticConfig(hosts: String, bulk: BulkConfig)

case class BulkConfig(maxElements: Int, timeWindow: FiniteDuration)
