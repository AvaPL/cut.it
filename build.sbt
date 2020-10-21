import sbt._

name := "cut.it"
ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.13.3"
ThisBuild / scalacOptions ++= Scalac.options

lazy val common = Projects.common

lazy val `basic-graphql` = Projects.`basic-graphql`

lazy val `cut-link` = Projects.`cut-link`

lazy val `link-store` = Projects.`link-store`

lazy val `integration-tests` = Projects.`integration-tests`
