package config

import pureconfig.{ConfigObjectSource, ConfigReader, ConfigSource}

/** Trait that allows a class to load a config of defined type.
  *
  * @tparam T
  *   returned config type
  */
trait Config[T] {
  private var loadedConfig: Option[T] = None

  /** Loads a config using provided `ConfigReader`. The easiest way to provide a
    * `ConfigReader` is to use `import pureconfig.generic.auto._`. It reads the
    * config only once so subsequent calls do not reload the config.
    *
    * @param cr
    *   a config reader of type `T`
    * @return
    *   loaded config of type `T`
    */
  def config(implicit cr: ConfigReader[T]): T = {
    if (loadedConfig.isEmpty)
      loadedConfig = Some(loadConfig)
    scribe.trace(s"$className config: ${loadedConfig.get}")
    loadedConfig.get
  }

  private def loadConfig(implicit cr: ConfigReader[T]) =
    configSource.load[T] match {
      case Right(config) =>
        scribe.debug(s"Loaded config for $className")
        config
      case Left(_) =>
        scribe.warn(
          s"Config for $className could not be loaded, using default config"
        )
        defaultConfig
    }

  private def className = this.getClass.getName

  /** The source of config parameters, defaults to `ConfigSource.default`. It
    * should be overridden in derived classes if other `ConfigSource` should be
    * used.
    *
    * @return
    *   config source that will be used to load the config
    */
  def configSource: ConfigObjectSource = ConfigSource.default

  /** Default config to use if it could not be loaded from `configSource`.
    * Throws a `RuntimeException` by default.
    *
    * @return
    *   default config of type `T`
    */
  def defaultConfig: T =
    sys.error(s"Default config for $className is undefined")
}
