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
  lazy val catsCore  = "org.typelevel" %% "cats-core" % Version.catsCore
  lazy val scalamock = "org.scalamock" %% "scalamock" % Version.scalamock % Test
  lazy val akkaStreamKafkaTestkit =
    "com.typesafe.akka" %% "akka-stream-kafka-testkit" % Version.akkaStreamKafkaTestkit % Test
  lazy val testcontainersScalaScalatest =
    "com.dimafeng" %% "testcontainers-scala-scalatest" % Version.testcontainersScalaScalatest % Test
  lazy val testcontainersScalaKafka =
    "com.dimafeng" %% "testcontainers-scala-kafka" % Version.testcontainersScalaKafka % Test
  lazy val testcontainersScalaElasticsearch =
    "com.dimafeng" %% "testcontainers-scala-elasticsearch" % Version.testcontainersScalaElasticsearch % Test

  object Version {
    object Common {
      val scalatest      = "3.2.0"
      val circe          = "0.13.0"
      val akka           = "2.6.14"
      val elastic4s      = "7.9.1"
      val alpakkaKafka   = "2.0.7"
      val testcontainers = "0.39.3"
    }

    val scalactic                        = Common.scalatest
    val scalatest                        = Common.scalatest
    val sangriaGraphql                   = "2.0.0"
    val sangriaCirce                     = "1.3.1"
    val circeGeneric                     = Common.circe
    val circeParser                      = Common.circe
    val akkaStream                       = Common.akka
    val akkaHttp                         = "10.2.4"
    val akkaHttpCirce                    = "1.34.0"
    val akkaStreamTestkit                = Common.akka
    val akkaStreamKafka                  = Common.alpakkaKafka
    val akkaHttpTestkit                  = "10.2.4"
    val scribe                           = "2.7.12"
    val pureconfig                       = "0.15.0"
    val elastic4sCore                    = Common.elastic4s
    val elastic4sJsonCirce               = Common.elastic4s
    val elastic4sClientAkka              = Common.elastic4s
    val catsCore                         = "2.0.0"
    val scalamock                        = "5.1.0"
    val akkaStreamKafkaTestkit           = Common.alpakkaKafka
    val testcontainersScalaScalatest     = Common.testcontainers
    val testcontainersScalaKafka         = Common.testcontainers
    val testcontainersScalaElasticsearch = Common.testcontainers
  }

  object Kit {
    lazy val scalatest = Seq(scalactic, Dependencies.scalatest, scalamock)
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
        circeParser,
        akkaStreamTestkit,
        akkaStreamKafkaTestkit
      )
    lazy val elastic4s =
      Seq(
        elastic4sCore,
        elastic4sJsonCirce,
        elastic4sClientAkka,
        circeGeneric,
        circeParser
      )
    lazy val testcontainers = Seq(
      testcontainersScalaScalatest,
      testcontainersScalaKafka,
      testcontainersScalaElasticsearch
    )
  }
}
