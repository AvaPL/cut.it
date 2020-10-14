package link.store.config

import links.kafka.KafkaConfig

case class LinkStoreConfig(kafka: KafkaConfig, elastic: ElasticConfig)
