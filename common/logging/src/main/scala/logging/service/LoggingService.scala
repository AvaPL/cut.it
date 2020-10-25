package logging.service

import logging.filter.ServiceFilter
import logging.model.levels.{Levels, LevelsChange}
import scribe.handler.LogHandler

case class LoggingService(
    servicePackage: String,
    private var _currentLevels: Levels
) {

  def changeLevels(levelsChange: LevelsChange): Levels = {
    val service   = levelsChange.service.getOrElse(currentLevels.service)
    val libraries = levelsChange.libraries.getOrElse(currentLevels.libraries)
    val levels    = Levels(service, libraries)
    changeLevels(levels)
  }

  def changeLevels(levels: Levels): Levels = {
    replaceScribeLogger(levels)
    _currentLevels = levels
    scribe.info(
      s"Logging levels set to [service: ${_currentLevels.service}, libraries: ${_currentLevels.libraries}]"
    )
    _currentLevels
  }

  private def replaceScribeLogger(levels: Levels) = {
    val filter = ServiceFilter(servicePackage, levels)
    scribe.Logger.root
      .clearHandlers()
      .withHandler(LogHandler.default)
      .setModifiers(List(filter))
      .replace()
  }

  def currentLevels: Levels = _currentLevels
}
