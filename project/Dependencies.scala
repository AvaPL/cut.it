import sbt._

object Dependencies {
  val scalactic = "org.scalactic"       %% "scalactic" % Version.scalactic
  val scalatest = "org.scalatest"       %% "scalatest" % Version.scalatest % "test"
  val sangria   = "org.sangria-graphql" %% "sangria"   % Version.sangria
  val sangriaCirce =
    "org.sangria-graphql" %% "sangria-circe" % Version.sangriaCirce
  val circeGeneric = "io.circe"          %% "circe-generic" % Version.circeGeneric
  val circeParser  = "io.circe"          %% "circe-parser"  % Version.circeParser
  val akkaStream   = "com.typesafe.akka" %% "akka-stream"   % Version.akkaStream
  val akkaHttp     = "com.typesafe.akka" %% "akka-http"     % Version.akkaHttp
  val akkaHttpCirce =
    "de.heikoseeberger" %% "akka-http-circe" % Version.akkaHttpCirce

  object Version {
    lazy val scalactic     = "3.2.0"
    lazy val scalatest     = "3.2.0"
    lazy val sangria       = "2.0.0"
    lazy val sangriaCirce  = "1.3.0"
    lazy val circeGeneric  = "0.13.0"
    lazy val circeParser   = "0.13.0"
    lazy val akkaStream    = "2.6.8"
    lazy val akkaHttp      = "10.2.0"
    lazy val akkaHttpCirce = "1.34.0"
  }

  object Kit {
    val scalatest = Seq(scalactic, Dependencies.scalatest)
    val sangria =
      Seq(Dependencies.sangria, sangriaCirce, circeGeneric, circeParser)
    val akkaHttp = Seq(Dependencies.akkaHttp, akkaStream, akkaHttpCirce)
  }
}
