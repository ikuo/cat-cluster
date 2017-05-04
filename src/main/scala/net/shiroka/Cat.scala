package net.shiroka

import java.util.Optional
import scala.concurrent.duration._
import akka.actor._
import akka.cluster._
import akka.cluster.sharding._
import akka.persistence._
import net.ceedubs.ficus.Ficus._
import cat.pb.cat._
import cat.pb.journal.sweeper._
import com.trueaccord.scalapb.{ GeneratedMessage => Message }

class Cat extends PersistentActor {
  import Cat._
  override val persistenceId: String = self.path.name
  private[this] var state = StateOps(State.defaultInstance)

  override def receiveCommand = {
    case msg @ Sweep(id) =>
      val ack =
        if (state.isExpired) { SweepAck(id) }
        else { SweepAck("") }
      sender ! ack
    case msg: Message => persist(msg)(updateState)
  }

  override def receiveRecover = {
    case msg: Message => updateState(msg)
  }

  def updateState(msg: Message): Unit = msg match {
    case msg: Meow => this.state = state.raw
      .update(_.numMeow.modify(_ + 1L))
      .update(_.lastEventAt := msg.posixTime)
    case _ =>
  }
}

object Cat extends Config {
  final val configKey = "cat"
  final val shardingName = "cat"
  final val shardingRole = "cat"
  val maxEntities = config.as[Int]("max-entities")
  val numberOfShards = config.as[Int]("num-of-shards")
  val timeToExpire = config.as[FiniteDuration]("time-to-expire").toSeconds
  val rememberEntities = config.as[Boolean]("remember-entities")

  implicit class StateOps(val raw: State) {
    def isExpired = (now - raw.lastEventAt) > timeToExpire
    def now = System.currentTimeMillis / 1000L
  }

  def startSharding(implicit system: ActorSystem): Unit =
    ClusterSharding(system).start(
      typeName = Cat.shardingName,
      entityProps = Props(classOf[Cat]),
      settings = ClusterShardingSettings(system)
        .withRole(shardingRole)
        .withRememberEntities(rememberEntities),
      messageExtractor = messageExtractor)

  def startProxy(implicit system: ActorSystem): Unit =
    ClusterSharding(system).startProxy(
      typeName = Cat.shardingName,
      role = Optional.of(shardingRole),
      messageExtractor = messageExtractor)

  val messageExtractor =
    new ShardRegion.HashCodeMessageExtractor(numberOfShards) {
      override def entityId(message: Any): String = message match {
        case msg: Meow => msg.catId
        case msg: Sweep => msg.persistenceId
      }
    }
}
