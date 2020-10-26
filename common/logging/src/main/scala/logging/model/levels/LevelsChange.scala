package logging.model.levels

import scribe.Level
import scribe.Level.{Debug, Error, Info, Trace, Warn}

case class LevelsChange(
    service: Option[Level] = None,
    libraries: Option[Level] = None
)

object LevelsChange {
  def fromStrings(
      service: Option[String] = None,
      libraries: Option[String] = None
  ): LevelsChange =
    LevelsChange(service.flatMap(parseLevel), libraries.flatMap(parseLevel))

  private def parseLevel(level: String) =
    level.toUpperCase match {
      case Trace.name => Some(Trace)
      case Debug.name => Some(Debug)
      case Info.name  => Some(Info)
      case Warn.name  => Some(Warn)
      case Error.name => Some(Error)
      case _          => None
    }
}
