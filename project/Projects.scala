import sbt._
import Dependencies._
import com.typesafe.sbt.SbtNativePackager.Docker
import com.typesafe.sbt.packager.Keys.{dockerBaseImage, dockerExposedPorts}
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt.Keys.{libraryDependencies, name, version}

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
    .enablePlugins(JavaAppPackaging)
    .settings(
      dockerBaseImage := "openjdk:14"
    )

  lazy val common = project
    .settings(
      name := "common"
    )

  object Common {
    lazy val config = project
      .settings(
        name := "config",
        libraryDependencies += pureconfig,
        libraryDependencies += scribe % Provided,
        libraryDependencies ++= Kit.scalatest
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
