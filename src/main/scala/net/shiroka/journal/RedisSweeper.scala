package net.shiroka.journal

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.mutable
import akka.event.LoggingAdapter
import akka.actor._
import akka.pattern.ask
import akka.cluster.sharding._
import akka.cluster.singleton._
import akka.persistence.redis.RedisUtils
import akka.persistence.query._
import akka.persistence.query.journal.redis._
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.config.ConfigFactory
import redis.RedisClient
import net.shiroka._
import cat.pb.journal.sweeper._

class RedisSweeper extends Actor with ActorLogging {
  import RedisSweeper._

  implicit val system = context.system
  implicit val ec = context.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val logger = log
  private[this] val regions = mutable.Map.empty[String, ActorRef]
  val sweepableId = """^(\w+)-.+""".r

  val readJournal = PersistenceQuery(system)
    .readJournalFor[ScalaReadJournal]("akka-persistence-redis.read-journal")
  val redis: RedisClient =
    RedisUtils.create(ConfigFactory.load.getConfig("akka-persistence-redis.journal"))

  override def preStart: Unit = {
    super.preStart()
    self ! Start
  }

  override def postStop: Unit = try {
    redis.stop()
  } finally {
    super.postStop()
  }

  def receive = {
    case Start => start
  }

  private def start: Unit = {
    import ActorAttributes.supervisionStrategy
    import Supervision.resumingDecider
    val profiler = context.actorOf(Props(classOf[IterationProfiler]), "iteration-profiler")

    readJournal.currentPersistenceIds
      .mapAsync(parallelism = 5)(id =>
        (id match {
          case id @ sweepableId(name) =>
            profiler ! id
            regions.getOrElseUpdate(name, ClusterSharding(system).shardRegion(name))
              .ask(Sweep(id))(2.seconds)
          case id => Future.failed(new RuntimeException(s"Malformed persistence Id: $id"))
        }).transform(identity, error("Failed to sweep entity"))
      ).withAttributes(supervisionStrategy(resumingDecider))
       .runWith(Sink.ignore)
       .map(_ => profiler ! 'Finish)
       .map(_ => system.scheduler.scheduleOnce(5.seconds, self, Start))
       .onFailure(error("Failed to sweep entities"))
  }
}

object RedisSweeper {
  private class IterationProfiler extends Actor {
    import java.io._
    import java.text.SimpleDateFormat
    private[this] var numIds: Long = 0
    private[this] var started: Long = now
    final val filename = "log/profile.sweeper.log"
    lazy val timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new java.util.Date)

    def receive = {
      case persistenceId: String => numIds += 1
      case 'Finish => finish
    }

    def finish: Unit = {
      val line = Seq(timestamp, sys.env("AKKA_HOSTNAME"), numIds, (now - started)).mkString("\t")
      val out = new PrintWriter(new FileOutputStream(new File(filename), true))
      try { out.println(line) } finally { out.close() }
      self ! PoisonPill
    }
  }

  def now: Long = System.currentTimeMillis() / 1000L // in POSIX time

  private def error(msg: String)(implicit log: LoggingAdapter): PartialFunction[Any, Throwable] =
    { case (err: Throwable) => log.error(err, msg); err }

  final object Start

  def startSingleton(name: String, role: Option[String] = None)(implicit system: ActorSystem): ActorRef =
    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(classOf[RedisSweeper]),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system).withRole(role)
      ),
      name = name
    )
}
