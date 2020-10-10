import Dependencies._
import com.typesafe.sbt.packager.Keys.dockerBaseImage
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt.Keys.{libraryDependencies, name}
import sbt._

object Projects {
  lazy val common = project
    .settings(
      name := "common"
    )

  lazy val `basic-graphql` = project.dockerize
    .settings(
      name := "basic-graphql",
      libraryDependencies ++= Kit.scalatest,
      libraryDependencies ++= Kit.sangria,
      libraryDependencies ++= Kit.akkaHttp,
      libraryDependencies += scribe % Provided
    )
    .dependsOn(Common.config)
    .dependsOn(Common.logging)

  lazy val `cut-link` = project.dockerize
    .settings(
      name := "cut-link",
      libraryDependencies ++= Kit.scalatest,
      libraryDependencies ++= Kit.sangria,
      libraryDependencies ++= Kit.akkaHttp,
      libraryDependencies += scribe % Provided,
      libraryDependencies ++= Kit.alpakkaKafka
    )
    .dependsOn(Common.config)
    .dependsOn(Common.logging)

  lazy val `link-store` = project.dockerize
    .settings(
      name := "link-store",
      libraryDependencies ++= Kit.scalatest,
      libraryDependencies ++= Kit.sangria,
      libraryDependencies ++= Kit.akkaHttp,
      libraryDependencies += scribe % Provided,
      libraryDependencies ++= Kit.alpakkaKafka
    )
    .dependsOn(Common.config)
    .dependsOn(Common.logging)

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

  implicit class ProjectExtensions(val project: Project) extends AnyVal {
    def dockerize: Project = project
      .enablePlugins(JavaAppPackaging)
      .settings(
        dockerBaseImage := "openjdk:14"
      )
  }
}
