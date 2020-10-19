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

  lazy val `link-store` = project.dockerize
    .settings(
      name := "link-store",
      libraryDependencies ++= Kit.scalatest,
      libraryDependencies ++= Kit.sangria,
      libraryDependencies ++= Kit.akkaHttp,
      libraryDependencies += scribeSlf4j % Provided,
      libraryDependencies ++= Kit.alpakkaKafka,
      libraryDependencies ++= Kit.elastic4s,
      libraryDependencies += catsCore,
      libraryDependencies ++= Kit.testcontainers
    )
    .dependsOn(Common.links)
    .dependsOn(Common.config)
    .dependsOn(Common.logging)

  object Common {
    lazy val config = project
      .settings(
        name := "config",
        libraryDependencies += pureconfig,
        libraryDependencies += scribeSlf4j % Provided,
        libraryDependencies ++= Kit.scalatest
      )

    lazy val logging = project
      .settings(
        name := "logging",
        libraryDependencies += scribeSlf4j,
        libraryDependencies ++= Kit.akkaHttp,
        libraryDependencies ++= Kit.scalatest
      )

    lazy val links = project
      .settings(
        name := "links",
        Test / fork := true,
        libraryDependencies += akkaStreamKafka,
        libraryDependencies += sangriaGraphql % Provided,
        libraryDependencies ++= Kit.scalatest,
        libraryDependencies ++= Kit.testcontainers,
        libraryDependencies += scribeSlf4j % Test
      )
  }

  implicit class ProjectOps(val project: Project) extends AnyVal {
    def dockerize: Project = project
      .enablePlugins(JavaAppPackaging)
      .settings(
        dockerBaseImage := "openjdk:14"
      )
  }
}
