package net.shiroka
import java.util.Random
import scala.collection.JavaConverters._
import akka.actor._
import com.typesafe.config.ConfigFactory

object Main {
  val roles = ConfigFactory.load.getConfig("akka.cluster").getStringList("roles").asScala
  val primaryRole: String = roles.headOption.getOrElse(sys.error("empty roles"))

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("cluster")
    Profiler.run(system)

    primaryRole match {
      case "seed" => Cat.startProxy
      case "cat" =>
        Cat.startSharding
        system.actorOf(Props(classOf[Sensor], new Random()), "cat") ! Sensor.Start
      case role => sys.error(s"Unexpected role $role")
    }

    TinyHttpServer.serve(8080)
  }
}
