package net.shiroka.cat.journal

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.mutable
import akka.event.LoggingAdapter
import akka.actor._
import akka.cluster.sharding._
import akka.cluster.singleton._
import akka.persistence.redis.{ RedisUtils, RedisKeys }
import akka.persistence.query._
import akka.persistence.query.journal.redis._
import akka.stream._
import akka.stream.scaladsl._
import akka.routing.RoundRobinPool
import com.typesafe.config.{ ConfigFactory, Config => TSConfig }
import redis.RedisClient
import redis.protocol.MultiBulk
import net.ceedubs.ficus.Ficus._
import net.shiroka.cat.Config
import net.shiroka.cat.pb.journal.sweeper._

class RedisSweeper extends Actor with ActorLogging {
  import akka.pattern.ask
  import RedisSweeper._

  implicit val system = context.system
  implicit val ec = context.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val logger = log
  private[this] val regions = mutable.Map.empty[String, ActorRef]
  val sweepableId = """^(\w+)-.+""".r

  val readJournal = PersistenceQuery(system)
    .readJournalFor[ScalaReadJournal]("akka-persistence-redis.read-journal")

  val workers = context.actorOf(RoundRobinPool(40).props(Props[DeleteWorker]), "delete-workers")

  override def preStart: Unit = {
    super.preStart()
    self ! Start
  }

  def receive = {
    case Start => start
  }

  private def start: Unit = {
    import ActorAttributes.supervisionStrategy
    import Supervision.resumingDecider
    import net.shiroka.cat.syntax.TapFailure
    val profiler = context.actorOf(Props(classOf[IterationProfiler]), "iteration-profiler")

    readJournal.currentPersistenceIds
      .mapAsyncUnordered(parallelism = 20)(id =>
        (id match {
          case id @ sweepableId(name) =>
            profiler ! 'Sweepable
            regions.getOrElseUpdate(name, ClusterSharding(system).shardRegion(name))
              .ask(Sweep(id))(timeout.sweepAck).mapTo[SweepAck]
              .flatMap(deleteMetadata)
              .map(_.foreach(_ => profiler ! 'Sweeped))
          case id => sys.error(s"Malformed persistence Id: $id")
        }).tapFailure(profiler ! _))
      .withAttributes(supervisionStrategy(resumingDecider))
      .runWith(Sink.ignore)
      .tapFailure(log.error("Failed to sweep entities", _))
      .onComplete {
        case _ =>
          profiler ! 'Finish
          system.scheduler.scheduleOnce(interval, self, Start)
      }
  }

  private def deleteMetadata(ack: SweepAck) = workers.ask(ack)(timeout.deleteMetadata).mapTo[Option[_]]
}

object RedisSweeper extends Config {
  val configKey = "journal.redis-sweeper"
  val stdout = config.as[Boolean]("stdout")
  val parallelism = config.as[Int]("parallelism")
  val interval = config.as[FiniteDuration]("interval")
  val profilerInterval = config.as[FiniteDuration]("profiler.interval")
  val warningTolerance = config.as[Double]("warning-tolerance")
  val timeout = Timeout(config.getConfig("timeout"))
  final object Start
  private final object Sweeped

  case class Timeout(config: TSConfig) {
    val sweepAck = config.as[FiniteDuration]("sweep-ack")
    val deleteMetadata = config.as[FiniteDuration]("delete-metadata")
  }

  private class DeleteWorker extends Actor {
    import akka.pattern.pipe
    implicit val system = context.system
    implicit val ec = context.dispatcher
    lazy val redis: RedisClient =
      RedisUtils.create(ConfigFactory.load.getConfig("akka-persistence-redis.journal"))

    def receive = {
      case SweepAck(id) => deleteMetadata(id).pipeTo(sender)
    }

    private[this] def deleteMetadata(id: String): Future[Option[_]] =
      if (id.nonEmpty) {
        import RedisKeys._
        val tx = redis.transaction()
        tx.del(highestSequenceNrKey(id))
        tx.srem(identifiersKey, id)
        tx.exec().map(Some(_))
      } else Future.successful(None)

    override def postStop: Unit = try redis.stop() finally super.postStop()
  }

  private class IterationProfiler() extends Actor with ActorLogging {
    import java.io._
    import java.text.SimpleDateFormat
    implicit val logger = log
    implicit val ec = context.dispatcher
    private[this] var numErrors: Long = 0
    private[this] var numSweepables: Long = 0
    private[this] var numSweeped: Long = 0
    private[this] var started: Long = now
    private[this] var scheduledEmit: Option[Cancellable] = None
    final val filename = "log/profile.sweeper.log"
    private def timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new java.util.Date)

    override def preStart: Unit = {
      scheduleEmit
      super.preStart()
    }

    def receive = {
      case 'Sweepable => numSweepables += 1
      case 'Sweeped => numSweeped += 1
      case 'Emit => { emit; scheduleEmit }
      case err: Throwable =>
        numErrors += 1
        if (tooManyErrors_?) log.warning("Failed to sweep entity. {}", err.getMessage)
      case 'Finish => { emit; finish }
    }

    private def scheduleEmit: Unit = {
      scheduledEmit.foreach(_.cancel)
      scheduledEmit = Some(context.system.scheduler.scheduleOnce(profilerInterval, self, 'Emit))
    }

    private def emit: Unit = {
      val line = Seq(
        timestamp, sys.env("AKKA_HOSTNAME"), started,
        numSweepables, numSweeped, numErrors,
        (now - started)
      ).mkString("\t")

      if (stdout) { println(line) }
      else {
        val out = new PrintWriter(new FileOutputStream(new File(filename), true))
        try { out.println(line) } finally { out.close() }
      }
    }

    private def finish: Unit = {
      scheduledEmit.foreach(_.cancel)
      self ! PoisonPill
    }

    private def tooManyErrors_? = (numSweepables > 0 && (numErrors / numSweepables.toDouble) > warningTolerance)
  }

  private def now: Long = System.currentTimeMillis() / 1000L // in POSIX time

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
