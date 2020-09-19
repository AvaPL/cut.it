package config

import pureconfig.{ConfigReader, ConfigSource}

// TODO: Make a separate module with this trait
trait Config[T] {
  private var loadedConfig: Option[T] = None

  def config(implicit cr: ConfigReader[T]): T = {
    if (loadedConfig.isEmpty)
      loadedConfig = Some(loadConfig)
    loadedConfig.get
  }

  private def loadConfig(implicit cr: ConfigReader[T]) =
    ConfigSource.default.load[T] match {
      case Right(config) =>
        scribe.debug(s"Loaded config for $className")
        config
      case Left(_) =>
        scribe.warn(
          s"Config for $className could not be loaded, using default config $defaultConfig"
        )
        defaultConfig
    }

  private def className =
    this.getClass.getName.dropRight(1) // Removes trailing '$'

  def defaultConfig: T = sys.error("Default config undefined")
}
