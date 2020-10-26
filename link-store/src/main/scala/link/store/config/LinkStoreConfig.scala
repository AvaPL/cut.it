package link.store.config

import kafka.KafkaConfig

case class LinkStoreConfig(
    port: Int,
    kafka: KafkaConfig,
    elastic: ElasticConfig
)
