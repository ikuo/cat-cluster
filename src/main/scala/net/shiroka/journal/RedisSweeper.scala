package net.shiroka.journal

import akka.actor._
import akka.cluster.singleton._
import akka.persistence.redis.RedisUtils
import akka.persistence.query._
import akka.persistence.query.journal.redis._
import akka.stream._
import akka.stream.scaladsl._
import com.typesafe.config.ConfigFactory
import redis.RedisClient

class RedisSweeper extends Actor with ActorLogging {
  import RedisSweeper._

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

  val readJournal = PersistenceQuery(system)
    .readJournalFor[ScalaReadJournal]("akka-persistence-redis.read-journal")

  private var redis: RedisClient = _

  override def preStart: Unit = {
    this.redis = RedisUtils.create(ConfigFactory.load.getConfig("akka-persistence-redis.journal"))
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
    readJournal.persistenceIds.runForeach(println)
  }
}

object RedisSweeper {
  case class Sweep(time: Long)
  object Start

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
