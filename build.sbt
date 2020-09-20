import sbt._

name := "cut.it"
version := "0.1"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / scalacOptions ++= Scalac.options

lazy val `basic-graphql` = Projects.`basic-graphql`

lazy val common = Projects.common
