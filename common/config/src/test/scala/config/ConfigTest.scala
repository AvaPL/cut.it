package config

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pureconfig.{ConfigObjectSource, ConfigSource}
import pureconfig.generic.auto._

class ConfigTest extends AnyWordSpec with Matchers {

  case class TestConfig(string: String, int: Int)

  "Config" when {
    "loading config" should {
      "throw exception for no config nor default config provided" in {
        val objectWithConfig = new Object with Config[TestConfig] {}
        a[RuntimeException] shouldBe thrownBy(objectWithConfig.config)
      }

      val testConfig = TestConfig("test", 123)

      "load default config for no config provided" in {
        val objectWithConfig = new Object with Config[TestConfig] {
          override def defaultConfig: TestConfig = testConfig
        }
        objectWithConfig.config should be(testConfig)
      }

      val testConfigString =
        """{
          | string = "test"
          | int = 123
          |}
          |""".stripMargin

      "load provided config" in {
        val testConfigSource = ConfigSource.string(testConfigString)
        val objectWithConfig = new Object with Config[TestConfig] {
          override def configSource: ConfigObjectSource = testConfigSource
        }
        objectWithConfig.config should be(testConfig)
      }

      "load provided config instead of default" in {
        val testConfigSource = ConfigSource.string(testConfigString)
        val objectWithConfig = new Object with Config[TestConfig] {
          override def configSource: ConfigObjectSource = testConfigSource
          override def defaultConfig: TestConfig        = TestConfig("default", 321)
        }
        objectWithConfig.config should be(TestConfig("test", 123))
      }
    }
  }
}
