package logging

import logging.filter.ServiceFilter
import logging.model.levels.Levels
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scribe.Level._

class LoggingTest extends AnyWordSpec with Matchers {

  "Logging" when {
    "started" should {
      s"set scribe to service level WARN and libraries level DEBUG" in {
        val loggingServicePackage = "logging.test"
        val loggingDefaultLoggingLevels =
          Levels(service = Warn, libraries = Debug)

        new Object with App with Logging {
          override def enableLoggingServer: Boolean = false
          override def servicePackage               = loggingServicePackage
          override def defaultLoggingLevels         = loggingDefaultLoggingLevels
        }

        checkServiceFilter(loggingServicePackage, loggingDefaultLoggingLevels)
      }
    }
  }

  private def checkServiceFilter(servicePackage: String, levels: Levels) = {
    val serviceFilter =
      scribe.Logger.root.modifiers.find(_.id == ServiceFilter.id) match {
        case Some(filter: ServiceFilter) => filter
        case _                           => fail("ServiceFilter not found on scribe logger")
      }
    serviceFilter.servicePackage should be(servicePackage)
    serviceFilter.levels should be(levels)
  }
}
