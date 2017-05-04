package net.shiroka

import java.util.Optional
import akka.actor._
import akka.cluster._
import akka.cluster.sharding._
import akka.persistence._
import net.ceedubs.ficus.Ficus._
import journal.RedisSweeper.Sweep

class Cat extends PersistentActor {
  import Cat._

  override val persistenceId: String = self.path.name

  private[this] var numMeow = 0

  override def receiveCommand = {
    case msg: Message => persist(msg)(updateState)
    case Sweep(id, posixTime) if (posixTime > 0) =>
      println(s"Sweeping ${id}, $posixTime ############################################################")
  }

  override def receiveRecover = {
    case msg: Message => updateState(msg)
  }

  def updateState(msg: Message): Unit = msg match {
    case msg: Meow => this.numMeow += 1
    case _ =>
  }
}

object Cat extends Config {
  final val configKey = "cat"
  final val shardingName = "cat"
  final val shardingRole = "cat"
  val maxEntities = config.as[Int]("max-entities")
  val numberOfShards = config.as[Int]("num-of-shards")
  val rememberEntities = config.as[Boolean]("remember-entities")

  trait Message { val catId: String }
  case class Meow(catId: String) extends Message
  case class GetMeows(catId: String) extends Message

  def startSharding(implicit system: ActorSystem): Unit = {
    ClusterSharding(system).start(
      typeName = Cat.shardingName,
      entityProps = Props(classOf[Cat]),
      settings = ClusterShardingSettings(system)
        .withRole(shardingRole)
        .withRememberEntities(rememberEntities),
      messageExtractor = messageExtractor)
  }

  def startProxy(implicit system: ActorSystem): Unit = {
    ClusterSharding(system).startProxy(
      typeName = Cat.shardingName,
      role = Optional.of(shardingRole),
      messageExtractor = messageExtractor)
  }

  val messageExtractor =
    new ShardRegion.HashCodeMessageExtractor(numberOfShards) {
      override def entityId(message: Any): String = message match {
        case msg: Message => msg.catId
        case msg: Sweep => msg.persistenceId
      }
    }
}
