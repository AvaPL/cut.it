package config

import pureconfig.{ConfigReader, ConfigSource}

trait Config[T] {
  private var loadedConfig: Option[T] = None

  def config(implicit cr: ConfigReader[T]): T = {
    if (loadedConfig.isEmpty)
      loadedConfig = Some(loadConfig)
    scribe.trace(s"$className config: ${loadedConfig.get}")
    loadedConfig.get
  }

  private def loadConfig(implicit cr: ConfigReader[T]) =
    ConfigSource.default.load[T] match {
      case Right(config) =>
        scribe.debug(s"Loaded config for $className")
        config
      case Left(_) =>
        scribe.warn(
          s"Config for $className could not be loaded, using default config"
        )
        defaultConfig
    }

  private def className =
    this.getClass.getName.dropRight(1) // Removes trailing '$'

  def defaultConfig: T =
    sys.error(s"Default config for $className is undefined")
}
