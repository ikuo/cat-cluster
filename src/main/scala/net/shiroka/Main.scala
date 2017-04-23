package net.shiroka
import akka.actor._

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("cluster")
    Profiler.run(system)
    TinyHttpServer.serve(8080)
  }
}
