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
  val hostName = sys.env.getOrElse("AKKA_HOSTNAME", "unknown")
  lazy val region = ClusterSharding(context.system).shardRegion(Cat.shardingName)
  implicit val ec: ExecutionContext = context.dispatcher
  val redis: RedisClient =
    RedisUtils.create(ConfigFactory.load.getConfig("akka-persistence-redis.journal"))
  var emitter: Option[ActorRef] = None

  def receive = {
    case stats: Stats => emit(stats)
    case stats: ClusterShardingStats =>
      redis.info("memory").map(r => emitter.getOrElse(self) ! Stats(stats, r))
    case Start =>
      system.scheduler.schedule(2.seconds, interval, region, GetClusterShardingStats(20.seconds))
    case Emitter(ref) => this.emitter = ref
  }

  def emit(stats: Stats): Unit = {
    val rt = Runtime.getRuntime
    val Stats(ClusterShardingStats(sharding), redis) = stats
    val numEntities = sharding.values.map(_.stats.values.sum).sum
    val used = rt.totalMemory - rt.freeMemory

    val line = Seq(
      now,
      hostName,
      if (Cat.rememberEntities) 1 else 0,
      numEntities,
      memString(redis.getOrElse("used_memory", "0").toLong),
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

    if (shutdownOnMemoryShortage && used.toDouble / rt.maxMemory > 0.85) {
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
  val shutdownOnMemoryShortage = false

  object Start

  case class Stats(sharding: ClusterShardingStats, redis: Map[String, String])
  object Stats {
    def apply(sharding: ClusterShardingStats, redis: String): Stats = {
      val entries = for {
        line <- redis.lines if (line.nonEmpty && line.head != '#')
        key :: value :: _ = line.split(":").toList
      } yield (key -> value)
      apply(sharding, Map(entries.toSeq: _*))
    }
  }

  case class Emitter(ref: Option[ActorRef])

  def run(system: ActorSystem): Unit = {
    system.actorOf(props) ! Start
  }

  def props = Props(classOf[Profiler])
}
