package net.shiroka.cat
import com.typesafe.config.ConfigFactory

trait Config {
  val configKey: String
  lazy val config = ConfigFactory.load.getConfig(s"net.shiroka.cat.$configKey")
}
