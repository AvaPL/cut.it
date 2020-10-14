import sbt._

object Dependencies {
  lazy val scalactic = "org.scalactic" %% "scalactic" % Version.scalactic % Test
  lazy val scalatest = "org.scalatest" %% "scalatest" % Version.scalatest % Test
  lazy val sangriaGraphql =
    "org.sangria-graphql" %% "sangria" % Version.sangriaGraphql
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
  lazy val scribeSlf4j = "com.outr" %% "scribe-slf4j" % Version.scribe
  lazy val pureconfig =
    "com.github.pureconfig" %% "pureconfig" % Version.pureconfig
  lazy val elastic4sCore =
    "com.sksamuel.elastic4s" %% "elastic4s-core" % Version.elastic4sCore
  lazy val elastic4sJsonCirce =
    "com.sksamuel.elastic4s" %% "elastic4s-json-circe" % Version.elastic4sJsonCirce
  lazy val elastic4sClientAkka =
    "com.sksamuel.elastic4s" %% "elastic4s-client-akka" % Version.elastic4sClientAkka

  object Version {
    object Common {
      val scalatest = "3.2.0"
      val circe     = "0.13.0"
      val akka      = "2.6.8"
      val elastic4s = "7.9.1"
    }

    val scalactic           = Common.scalatest
    val scalatest           = Common.scalatest
    val sangriaGraphql      = "2.0.0"
    val sangriaCirce        = "1.3.0"
    val circeGeneric        = Common.circe
    val circeParser         = Common.circe
    val akkaStream          = Common.akka
    val akkaHttp            = "10.2.0"
    val akkaHttpCirce       = "1.34.0"
    val akkaStreamTestkit   = Common.akka
    val akkaStreamKafka     = "2.0.5"
    val akkaHttpTestkit     = "10.2.0"
    val scribe              = "2.7.12"
    val pureconfig          = "0.13.0"
    val elastic4sCore       = Common.elastic4s
    val elastic4sJsonCirce  = Common.elastic4s
    val elastic4sClientAkka = Common.elastic4s
  }

  object Kit {
    lazy val scalatest = Seq(scalactic, Dependencies.scalatest)
    lazy val sangria =
      Seq(
        Dependencies.sangriaGraphql,
        sangriaCirce,
        circeGeneric,
        circeParser % Test
      )
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
    lazy val alpakkaKafka =
      Seq(
        akkaStream,
        akkaStreamKafka,
        circeGeneric,
        circeParser
      )
    lazy val elastic4s =
      Seq(
        elastic4sCore,
        elastic4sJsonCirce,
        elastic4sClientAkka,
        circeGeneric,
        circeParser
      )

    implicit class KitOps(val kit: Seq[ModuleID]) extends AnyVal {
      def %(scope: Configuration): Seq[ModuleID] = kit.map(_ % scope)
    }
  }
}
