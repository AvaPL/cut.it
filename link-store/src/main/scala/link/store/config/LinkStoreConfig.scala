package link.store.config

import links.kafka.KafkaConfig

case class LinkStoreConfig(
    port: Int,
    kafka: KafkaConfig,
    elastic: ElasticConfig
)
