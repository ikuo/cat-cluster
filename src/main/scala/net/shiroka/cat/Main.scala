package net.shiroka.cat
import java.util.Random
import scala.collection.JavaConverters._
import akka.actor._
import com.typesafe.config.ConfigFactory

object Main {
  val roles = ConfigFactory.load.getConfig("akka.cluster").getStringList("roles").asScala
  val primaryRole: String = roles.headOption.getOrElse(sys.error("empty roles"))

  def main(args: Array[String]): Unit = {
    implicit lazy val system = ActorSystem("cluster")

    primaryRole match {
      case "seed" =>
        Cat.startProxy

      case "sensor" =>
        Cat.startProxy
        system.actorOf(Props(classOf[Sensor], new Random()), "cat")

      case "cat" =>
        Cat.startSharding
        journal.RedisSweeper.startSingleton("redis-sweeper", Some("cat"))

      case role => sys.error(s"Unexpected role $role")
    }

    Profiler.run(system)
    TinyHttpServer.serve(8080)
  }
}
