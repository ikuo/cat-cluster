package net.shiroka

import akka.actor._
import akka.cluster.sharding._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import java.io._
import java.util.Date
import java.text.SimpleDateFormat
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import ShardRegion._

class Profiler extends Actor {
  import Profiler._

  val system = context.system
  val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
  lazy val region = ClusterSharding(context.system).shardRegion(Cat.shardingName)
  implicit val ec: ExecutionContext = context.dispatcher

  def receive = {
    case ClusterShardingStats(stats) => profile(stats)
    case Start =>
      system.scheduler.schedule(2.seconds, interval, region, GetClusterShardingStats(20.seconds))
  }

  def profile(stats: Map[Address, ShardRegionStats]): Unit = {
    val rt = Runtime.getRuntime
    val numEntities = stats.values.map(_.stats.values.sum).sum

    val line = Seq(
      now,
      numEntities,
      memString(rt.totalMemory - rt.freeMemory),
      memString(rt.totalMemory),
      memString(rt.freeMemory),
      memString(rt.maxMemory)
    ).mkString("\t")

    if (stdout) {
      println(line)
    } else {
      val out = new PrintWriter(new FileOutputStream(new File(filename), true))
      try { out.println(line) }
      finally { out.close() }
    }
  }

  def memString(usage: Long) = "%.6f".format(usage.toDouble * 1e-6)

  def now = df.format(new Date())
}

object Profiler {
  val config = ConfigFactory.load.getConfig("net.shiroka.profiler")
  val interval = config.as[FiniteDuration]("interval")
  val stdout = config.as[Boolean]("stdout")
  val filename = "log/profile.log"

  object Start

  def run(system: ActorSystem): Unit = {
    system.actorOf(props) ! Start
  }

  def props = Props(classOf[Profiler])
}
