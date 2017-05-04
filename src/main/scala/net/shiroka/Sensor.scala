package net.shiroka

import java.util.Random
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.actor._
import akka.cluster._
import akka.cluster.sharding._
import akka.cluster.ClusterEvent._
import net.ceedubs.ficus.Ficus._

class Sensor(val random: Random = new Random()) extends Actor {
  import Sensor._

  implicit val ec: ExecutionContext = context.dispatcher
  private var started: Option[Cancellable] = None

  val system = context.system
  val cat = ClusterSharding(context.system).shardRegion(Cat.shardingName)
  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp], classOf[MemberEvent])
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case Sense => (0 until batchSize).foreach(_ => cat ! Cat.Meow(randomCatId))
    case state: CurrentClusterState =>
      state.members
        .find(member => member.status == MemberStatus.Up && member.address == cluster.selfAddress)
        .foreach(_ => self ! Start)
    case MemberUp(member) =>
      if (member.address == cluster.selfAddress) { self ! Start }
    case msg: MemberEvent =>
      if (cluster.state.unreachable.size == 0) { self ! Start }
      else { stop }
    case Start => start
  }

  private def stop: Unit = {
    this.started.foreach(_.cancel)
    this.started = None
  }

  private def start: Unit =
    if (started.isEmpty) {
      this.started = Some(system.scheduler.schedule(2.seconds, interval, self, Sense))
    }

  private def randomCatId: String = s"cat-${ random.nextInt(Cat.maxEntities) }"
}

object Sensor extends Config {
  val configKey = "sensor"
  val interval = config.as[FiniteDuration]("interval")
  val batchSize = config.as[Int]("batch.size")
  object Start
  object Sense
}
