package net.shiroka

import java.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.actor._
import akka.cluster._
import akka.cluster.sharding._
import net.ceedubs.ficus.Ficus._
import com.typesafe.config.ConfigFactory

class Sensor(val random: Random = new Random()) extends Actor {
  import Sensor._
  implicit val ec: ExecutionContext = context.dispatcher

  val system = context.system
  val cat = ClusterSharding(context.system).shardRegion(Cat.shardingName)

  def receive = {
    case Start => start
    case Sense => (0 until batchSize).foreach(_ => cat ! Cat.Meow(randomCatId))
  }

  private def start: Unit =
    system.scheduler.schedule(2.seconds, interval, self, Sense)

  private def randomCatId: String = s"cat-${ random.nextInt(maxCats) }"
}

object Sensor {
  val config = ConfigFactory.load.getConfig("net.shiroka.sensor")
  val interval = config.as[FiniteDuration]("interval")
  val batchSize = config.as[Int]("batch.size")
  val maxCats = config.as[Int]("cats.max")
  object Start
  object Sense
}
