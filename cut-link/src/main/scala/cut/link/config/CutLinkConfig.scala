package cut.link.config

import links.kafka.KafkaConfig

case class CutLinkConfig(port: Int, kafka: KafkaConfig)
