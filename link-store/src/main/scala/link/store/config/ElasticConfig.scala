package link.store.config

import scala.concurrent.duration.FiniteDuration

case class ElasticConfig(hosts: String, bulk: BulkConfig) {
  val hostsSeq: Seq[String] = hosts.split(',')
}

case class BulkConfig(maxElements: Int, timeWindow: FiniteDuration)
