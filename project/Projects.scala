import sbt._
import Dependencies._
import sbt.Keys.{libraryDependencies, name}

object Projects {
  lazy val `basic-graphql` = project
    .settings(
      name := "basic-graphql",
      libraryDependencies ++= Kit.scalatest,
      libraryDependencies ++= Kit.sangria,
      libraryDependencies ++= Kit.akkaHttp,
      libraryDependencies += scribe % Provided
    )
    .dependsOn(Common.config)
    .dependsOn(Common.logging)

  lazy val common = project
    .settings(
      name := "common"
    )

  object Common {
    lazy val config = project
      .settings(
        name := "config",
        libraryDependencies += pureconfig,
        libraryDependencies += scribe % Provided
      )

    lazy val logging = project
      .settings(
        name := "logging",
        libraryDependencies += scribe,
        libraryDependencies ++= Kit.akkaHttp,
        libraryDependencies ++= Kit.scalatest
      )
  }
}
