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
      libraryDependencies += scribe // TODO: Remove after adding logging module
    )
    .dependsOn(Common.config)

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
  }
}
