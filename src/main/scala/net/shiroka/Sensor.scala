package net.shiroka

import java.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.actor._
import akka.cluster._
import akka.cluster.sharding._

class Sensor(val random: Random = new Random()) extends Actor {
  import Sensor._
  implicit val ec: ExecutionContext = context.dispatcher

  val system = context.system
  val cat = ClusterSharding(context.system).shardRegion(Cat.shardingName)

  def receive = {
    case Start => start
  }

  private def start: Unit =
    system.scheduler.schedule(2.seconds, interval.seconds, cat, Cat.Meow(randomCatId))

  private def randomCatId: String = s"cat-${ random.nextInt(maxCats) }"
}

object Sensor {
  val interval = 1
  val maxCats = 10000
  object Start
}
