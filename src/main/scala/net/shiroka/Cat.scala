package net.shiroka

import java.util.Optional
import akka.actor._
import akka.cluster._
import akka.cluster.sharding._
import net.ceedubs.ficus.Ficus._

class Cat extends Actor {
  import Cat._

  private var numMeow = 0

  def receive = {
    case msg: Meow => this.numMeow += 1
  }
}

object Cat extends Config {
  val configKey = "cat"
  val shardingName = "cat"
  val shardingRole = "cat"
  val maxNumberOfShards = config.as[Int]("max-num-of-shards")
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
    new ShardRegion.HashCodeMessageExtractor(maxNumberOfShards) {
      override def entityId(message: Any): String = message match {
        case msg: Message => msg.catId
      }
    }
}
