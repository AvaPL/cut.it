import sbt._
import Dependencies._

name := "cut.it"
version := "0.1"
ThisBuild / scalaVersion := "2.13.3"

lazy val graphql = project.settings(
  name := "graphql",
  libraryDependencies ++= Kit.scalatest,
  libraryDependencies ++= Kit.sangria,
  libraryDependencies ++= Kit.akkaHttp
)
