package runner

import scribe.Level

// TODO: Move this class to a separate module
trait Runner extends App {
  scribe.Logger.root
    .clearHandlers()
    .clearModifiers()
    .withHandler(minimumLevel = Some(logLevel))
    .replace()

  def logLevel: Level = Level.Debug
}
