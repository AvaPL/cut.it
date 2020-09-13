import sbt._

object Dependencies {
  val scalactic = "org.scalactic" %% "scalactic" % Version.scalactic
  val scalatest = "org.scalatest" %% "scalatest" % Version.scalatest % "test"
  val sangria = "org.sangria-graphql" %% "sangria" % Version.sangria
  val sangriaCirce =
    "org.sangria-graphql" %% "sangria-circe" % Version.sangriaCirce
  val circeGeneric = "io.circe" %% "circe-generic" % Version.circeGeneric

  object Version {
    lazy val scalactic = "3.2.0"
    lazy val scalatest = "3.2.0"
    lazy val sangria = "2.0.0"
    lazy val sangriaCirce = "1.3.0"
    lazy val circeGeneric = "0.13.0"
  }

  object Kit {
    val scalatest = Seq(scalactic, Dependencies.scalatest)
    val sangria = Seq(Dependencies.sangria, sangriaCirce, circeGeneric)
  }
}
