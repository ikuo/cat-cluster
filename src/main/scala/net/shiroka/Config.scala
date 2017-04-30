package net.shiroka
import com.typesafe.config.ConfigFactory

trait Config {
  val configKey: String
  lazy val config = ConfigFactory.load.getConfig(s"net.shiroka.$configKey")
}
