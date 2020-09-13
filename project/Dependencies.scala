import sbt._

object Dependencies {
  val scalactic = "org.scalactic" %% "scalactic" % Version.scalactic
  val scalatest = "org.scalatest" %% "scalatest" % Version.scalatest % "test"

  object Version {
    lazy val scalactic = "3.2.0"
    lazy val scalatest = "3.2.0"
  }

  object Kit {
    val scalatest = Seq(scalactic, Dependencies.scalatest)
  }
}
