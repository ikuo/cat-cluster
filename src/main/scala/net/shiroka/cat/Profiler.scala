package net.shiroka.cat

import akka.actor._
import akka.cluster._
import akka.cluster.sharding._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import java.io._
import java.util.Date
import java.text.SimpleDateFormat
import akka.persistence.redis.{ RedisUtils, RedisKeys }
import com.typesafe.config.ConfigFactory
import redis.RedisClient
import net.ceedubs.ficus.Ficus._
import ShardRegion._

class Profiler extends Actor {
  import Profiler._

  implicit val system = context.system
  val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
  val hostName = sys.env("AKKA_HOSTNAME")
  lazy val region = ClusterSharding(context.system).shardRegion(Cat.shardingName)
  implicit val ec: ExecutionContext = context.dispatcher
  val redis: RedisClient =
    RedisUtils.create(ConfigFactory.load.getConfig("akka-persistence-redis.journal"))

  def receive = {
    case stats: Stats => emit(stats)
    case ClusterShardingStats(stats) =>
      redis.info("memory").map(r => self ! Stats(stats, r))
    case Start =>
      system.scheduler.schedule(2.seconds, interval, region, GetClusterShardingStats(20.seconds))
  }

  def emit(stats: Stats): Unit = {
    val rt = Runtime.getRuntime
    val numEntities = stats.sharding.values.map(_.stats.values.sum).sum
    val used = rt.totalMemory - rt.freeMemory

    val line = Seq(
      now,
      hostName,
      if (Cat.rememberEntities) 1 else 0,
      numEntities,
      memString(used),
      memString(rt.totalMemory),
      memString(rt.freeMemory),
      memString(rt.maxMemory)
    ).mkString("\t")

    if (stdout) {
      println(line)
    } else {
      val out = new PrintWriter(new FileOutputStream(new File(filename), true))
      try { out.println(line) } finally { out.close() }
    }

    if (used.toDouble / rt.maxMemory > 0.85) {
      println("System.exit() due to too much memory usage")
      sys.exit(1)
    }
  }

  def memString(usage: Long) = "%.6f".format(usage.toDouble * 1e-6)

  def now = df.format(new Date())
}

object Profiler extends Config {
  val configKey = "profiler"
  val interval = config.as[FiniteDuration]("interval")
  val stdout = config.as[Boolean]("stdout")
  val filename = "log/profile.log"

  object Start

  case class Stats(sharding: Stats.Sharding, redis: String)
  object Stats {
    type Sharding = Map[Address, ShardRegionStats]
  }

  def run(system: ActorSystem): Unit = {
    system.actorOf(props) ! Start
  }

  def props = Props(classOf[Profiler])
}
