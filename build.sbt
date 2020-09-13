import sbt._
import Dependencies._

name := "cut.it"
version := "0.1"
scalaVersion := "2.13.3"

lazy val graphql = project.settings(
  name := "GraphQL",
  libraryDependencies ++= Kit.scalatest
)
