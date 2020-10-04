import sbt._

object DebugDependencies {
  lazy val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j"      % Version.akkaSlf4j
  lazy val logback   = "ch.qos.logback"     % "logback-classic" % Version.logback

  object Version {
    val akkaSlf4j = Dependencies.Version.Common.akka
    val logback   = "1.2.3"
  }

  object Kit {
    lazy val akkaSlf4j = Seq(DebugDependencies.akkaSlf4j, logback)
  }
}
