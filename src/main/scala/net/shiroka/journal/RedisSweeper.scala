package net.shiroka.journal

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.mutable
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

class RedisSweeper extends Actor with ActorLogging {
  import RedisSweeper._

  implicit val system = context.system
  implicit val ec = context.dispatcher
  implicit val materializer = ActorMaterializer()
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
    var numIds: Long = 0
    readJournal.currentPersistenceIds.mapAsync(parallelism = 5)(_ match {
      case id @ sweepableId(name) =>
        val region = regions.getOrElseUpdate(name, ClusterSharding(system).shardRegion(name))
        numIds += 1
        region.ask(Sweep(id))(2.seconds)
      case id =>
        Future.failed(new RuntimeException(s"Malformed persistence Id: $id"))
    }).runWith(Sink.ignore)
      .map(_ => system.scheduler.scheduleOnce(5.seconds, self, Start))
      .onFailure { case err => log.error(err, "Failed to sweep entities") }
  }
}

object RedisSweeper {
  case class Sweep(persistenceId: String, posixTime: Long)
  object Sweep {
    def apply(id: String): Sweep = apply(id, System.currentTimeMillis() / 1000L)
  }

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
