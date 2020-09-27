import sbt._

object Dependencies {
  lazy val scalactic = "org.scalactic"       %% "scalactic" % Version.scalactic % Test
  lazy val scalatest = "org.scalatest"       %% "scalatest" % Version.scalatest % Test
  lazy val sangria   = "org.sangria-graphql" %% "sangria"   % Version.sangria
  lazy val sangriaCirce =
    "org.sangria-graphql" %% "sangria-circe" % Version.sangriaCirce
  lazy val circeGeneric = "io.circe" %% "circe-generic" % Version.circeGeneric
  lazy val circeParser  = "io.circe" %% "circe-parser"  % Version.circeParser
  lazy val akkaStream =
    "com.typesafe.akka" %% "akka-stream" % Version.akkaStream
  lazy val akkaStreamKafka =
    "com.typesafe.akka" %% "akka-stream-kafka" % Version.akkaStreamKafka
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
  lazy val akkaHttpCirce =
    "de.heikoseeberger" %% "akka-http-circe" % Version.akkaHttpCirce
  lazy val akkaStreamTestkit =
    "com.typesafe.akka" %% "akka-stream-testkit" % Version.akkaStreamTestkit % Test
  lazy val akkaHttpTestkit =
    "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttpTestkit % Test
  lazy val scribe = "com.outr" %% "scribe" % Version.scribe
  lazy val pureconfig =
    "com.github.pureconfig" %% "pureconfig" % Version.pureconfig

  object Version {
    val scalactic         = "3.2.0"
    val scalatest         = "3.2.0"
    val sangria           = "2.0.0"
    val sangriaCirce      = "1.3.0"
    val circeGeneric      = "0.13.0"
    val circeParser       = "0.13.0"
    val akkaStream        = "2.6.8"
    val akkaHttp          = "10.2.0"
    val akkaHttpCirce     = "1.34.0"
    val akkaStreamTestkit = "2.6.8"
    val akkaStreamKafka   = "2.0.5"
    val akkaHttpTestkit   = "10.2.0"
    val scribe            = "2.7.12"
    val pureconfig        = "0.13.0"
  }

  object Kit {
    lazy val scalatest = Seq(scalactic, Dependencies.scalatest)
    lazy val sangria =
      Seq(Dependencies.sangria, sangriaCirce, circeGeneric, circeParser % Test)
    lazy val akkaHttp =
      Seq(
        Dependencies.akkaHttp,
        akkaStream,
        akkaHttpCirce,
        circeGeneric,
        circeParser % Test,
        akkaStreamTestkit,
        akkaHttpTestkit
      )
    lazy val alpakkaKafka = Seq(akkaStream, akkaStreamKafka)
  }
}
