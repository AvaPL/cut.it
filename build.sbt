import sbt._
import Dependencies._

name := "cut.it"
version := "0.1"
ThisBuild / scalaVersion := "2.13.3"

lazy val `basic-graphql` = project
  .settings(
    name := "basic-graphql",
    libraryDependencies ++= Kit.scalatest,
    libraryDependencies ++= Kit.sangria,
    libraryDependencies ++= Kit.akkaHttp,
    libraryDependencies += scribe,
    libraryDependencies += pureconfig
  )
