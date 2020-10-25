package logging.filter

import logging.model.levels.Levels
import scribe.{LogRecord, Priority}
import scribe.filter.{Filter, PackageNameFilter}
import scribe.modify.{LevelFilter, LogModifier}

case class ServiceFilter(servicePackage: String, levels: Levels)
    extends LogModifier
    with Filter {
  override def id: String = ServiceFilter.id

  override def priority: Priority = Priority.Low

  private val packageNameFilter    = PackageNameFilter.startsWith(servicePackage)
  private val serviceLevelFilter   = LevelFilter >= levels.service
  private val librariesLevelFilter = LevelFilter >= levels.libraries

  override def apply[M](record: LogRecord[M]): Option[LogRecord[M]] =
    Option.when(matches(record))(record)

  override def matches[M](record: LogRecord[M]): Boolean =
    matchesService(record) || matchesLibraries(record)

  private def matchesService[M](record: LogRecord[M]) =
    serviceLevelFilter.matches(record) && packageNameFilter.matches(record)

  private def matchesLibraries[M](record: LogRecord[M]) =
    librariesLevelFilter.matches(record) && !packageNameFilter.matches(record)
}

object ServiceFilter {
  val id = "ServiceFilter"
}
