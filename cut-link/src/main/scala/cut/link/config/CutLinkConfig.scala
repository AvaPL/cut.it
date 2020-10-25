package cut.link.config

import kafka.KafkaConfig

case class CutLinkConfig(port: Int, kafka: KafkaConfig)
