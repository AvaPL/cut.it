package logging.filter

import logging.model.levels.Levels
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scribe.Level._
import scribe.{Level, LogRecord}

class ServiceFilterTest extends AnyWordSpec with Matchers {
  "ServiceFilter" when {
    val servicePackage = "filter.test"
    val libraryPackage = "library.test"

    "set to service level INFO and libraries level WARN" should {
      val serviceFilter =
        ServiceFilter(servicePackage, Levels(service = Info, libraries = Warn))

      "match service INFO record" in {
        val record = logRecord(servicePackage, Info)

        matchesRecord(serviceFilter, record)
      }

      "match service WARN record" in {
        val record = logRecord(servicePackage, Warn)

        matchesRecord(serviceFilter, record)
      }

      "not match service DEBUG record" in {
        val record = logRecord(servicePackage, Debug)

        doesNotMatchRecord(serviceFilter, record)
      }

      "match library WARN record" in {
        val record = logRecord(libraryPackage, Warn)

        matchesRecord(serviceFilter, record)
      }

      "match library ERROR record" in {
        val record = logRecord(libraryPackage, Error)

        matchesRecord(serviceFilter, record)
      }

      "not match library INFO record" in {
        val record = logRecord(libraryPackage, Info)

        doesNotMatchRecord(serviceFilter, record)
      }
    }

    "set to service level WARN and libraries level DEBUG" should {
      val serviceFilter =
        ServiceFilter(servicePackage, Levels(service = Warn, libraries = Debug))

      "match service WARN record" in {
        val record = logRecord(servicePackage, Warn)

        matchesRecord(serviceFilter, record)
      }

      "match service ERROR record" in {
        val record = logRecord(servicePackage, Error)

        matchesRecord(serviceFilter, record)
      }

      "not match service INFO record" in {
        val record = logRecord(servicePackage, Info)

        doesNotMatchRecord(serviceFilter, record)
      }

      "match library DEBUG record" in {
        val record = logRecord(libraryPackage, Debug)

        matchesRecord(serviceFilter, record)
      }

      "match library ERROR record" in {
        val record = logRecord(libraryPackage, Error)

        matchesRecord(serviceFilter, record)
      }

      "not match library TRACE record" in {
        val record = logRecord(libraryPackage, Trace)

        doesNotMatchRecord(serviceFilter, record)
      }
    }
  }

  private def matchesRecord(
      serviceFilter: ServiceFilter,
      record: LogRecord[String]
  ) = {
    serviceFilter.matches(record) should be(true)
    serviceFilter(record) should not be empty
  }

  private def doesNotMatchRecord(
      serviceFilter: ServiceFilter,
      record: LogRecord[String]
  ) = {
    serviceFilter.matches(record) should be(false)
    serviceFilter(record) shouldBe empty
  }

  private def logRecord(rootPackage: String, level: Level) =
    LogRecord.simple(() => "", "file", s"$rootPackage.record", level = level)
}
