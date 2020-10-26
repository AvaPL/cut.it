import Dependencies._
import com.typesafe.sbt.packager.Keys.dockerBaseImage
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt.Keys._
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
      libraryDependencies += scribeSlf4j % Provided
    )
    .dependsOn(Common.config)
    .dependsOn(Common.logging)

  lazy val `cut-link` = project.dockerize
    .settings(
      name := "cut-link",
      libraryDependencies ++= Kit.scalatest,
      libraryDependencies ++= Kit.sangria,
      libraryDependencies ++= Kit.akkaHttp,
      libraryDependencies += scribeSlf4j % Provided,
      libraryDependencies ++= Kit.alpakkaKafka
    )
    .dependsOn(Common.links)
    .dependsOn(Common.config)
    .dependsOn(Common.logging)
    .dependsOn(Common.kafka)
    .dependsOn(Common.graphql)

  lazy val `link-store` = project.dockerize.includeContainerTests
    .settings(
      name := "link-store",
      libraryDependencies ++= Kit.scalatest,
      libraryDependencies ++= Kit.sangria,
      libraryDependencies ++= Kit.akkaHttp,
      libraryDependencies += scribeSlf4j % Provided,
      libraryDependencies ++= Kit.alpakkaKafka,
      libraryDependencies ++= Kit.elastic4s,
      libraryDependencies += catsCore
    )
    .dependsOn(Common.links)
    .dependsOn(Common.config)
    .dependsOn(Common.logging)
    .dependsOn(Common.kafka)

  lazy val `integration-tests` = project.includeContainerTests
    .settings(
      name := "integration-tests",
      libraryDependencies ++= Kit.scalatest,
      libraryDependencies += akkaStreamTestkit,
      libraryDependencies += akkaHttpTestkit
    )
    .dependsOn(`cut-link`)
    .dependsOn(`link-store`)

  object Common {
    lazy val config = project
      .settings(
        name := "config",
        libraryDependencies += pureconfig,
        libraryDependencies += scribeSlf4j % Provided,
        libraryDependencies ++= Kit.scalatest
      )

    lazy val graphql = project
      .settings(
        name := "graphql",
        libraryDependencies ++= Kit.sangria,
        libraryDependencies ++= Kit.akkaHttp,
        libraryDependencies += scribeSlf4j % Provided,
        libraryDependencies ++= Kit.scalatest
      )

    lazy val logging = project
      .settings(
        name := "logging",
        libraryDependencies += scribeSlf4j,
        libraryDependencies ++= Kit.scalatest
      )
      .dependsOn(graphql)

    lazy val kafka = project.includeContainerTests
      .settings(
        name := "kafka",
        libraryDependencies += akkaStreamKafka,
        libraryDependencies ++= Kit.scalatest,
        libraryDependencies += scribeSlf4j % Test
      )

    lazy val links = project
      .settings(
        name := "links",
        libraryDependencies += sangriaGraphql % Provided
      )
  }

  implicit class ProjectOps(val project: Project) extends AnyVal {
    def dockerize: Project = project
      .enablePlugins(JavaAppPackaging)
      .settings(
        dockerBaseImage := "openjdk:14"
      )

    def includeContainerTests: Project = project
      .settings(
        Test / fork := true,
        libraryDependencies ++= Kit.testcontainers
      )
  }
}
