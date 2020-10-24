package logging.service

import logging.model.levels.{Levels, LevelsChange}
import scribe.Level._

case class LoggingService() {
  def setMinimumLoggingLevels(levelsChange: LevelsChange): Levels = {
    // TODO: Implement
    println(levelsChange)
    Levels(Info, Info)
  }

  def currentLevels: Levels =
    // TODO: Implement
    Levels(Info, Info)
}
