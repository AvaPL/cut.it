package logging.service

import logging.filter.ServiceFilter
import logging.model.levels.{Levels, LevelsChange}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scribe.Level._

class LoggingServiceTest extends AnyWordSpec with Matchers {
  val servicePackage = "filter.test"

  "LoggingService" when {
    "started" should {
      "set filter to given package and levels" in {
        createLoggingService
      }
    }

    "changeLevels is called" should {
      "set filter to given levels" in {
        val loggingService = createLoggingService

        val targetLevels = Levels(service = Warn, libraries = Debug)
        loggingService.changeLevels(targetLevels)

        checkServiceFilter(targetLevels)
      }

      "change only service level" in {
        val loggingService = createLoggingService

        val levelsChange = LevelsChange(service = Some(Warn))
        loggingService.changeLevels(levelsChange)

        checkServiceFilter(Levels(Warn, loggingService.currentLevels.libraries))
      }

      "change only libraries level" in {
        val loggingService = createLoggingService

        val levelsChange = LevelsChange(libraries = Some(Trace))
        loggingService.changeLevels(levelsChange)

        checkServiceFilter(Levels(loggingService.currentLevels.service, Trace))
      }
    }
  }

  private def createLoggingService = {
    val initialLevels  = Levels(service = Info, libraries = Warn)
    val loggingService = LoggingService(servicePackage, initialLevels)
    checkServiceFilter(initialLevels)
    loggingService
  }

  private def checkServiceFilter(levels: Levels) = {
    val serviceFilter =
      scribe.Logger.root.modifiers.find(_.id == ServiceFilter.id) match {
        case Some(filter: ServiceFilter) => filter
        case _                           => fail("ServiceFilter not found on scribe logger")
      }
    serviceFilter.servicePackage should be(servicePackage)
    serviceFilter.levels should be(levels)
  }
}
