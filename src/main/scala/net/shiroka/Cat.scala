package net.shiroka

import java.util.Optional
import akka.actor._
import akka.cluster._
import akka.cluster.sharding._

class Cat extends Actor {
  import Cat._

  private var numMeow = 0

  def receive = {
    case msg: Meow => this.numMeow += 1
  }
}

object Cat {
  val shardingName = "cat"
  val shardingRole = "cat"
  val maxNumberOfShards = 200

  trait Message { val catId: String }
  case class Meow(catId: String) extends Message
  case class GetMeows(catId: String) extends Message

  def startSharding(implicit system: ActorSystem): Unit = {
    ClusterSharding(system).start(
      typeName = Cat.shardingName,
      entityProps = Props(classOf[Cat]),
      settings = ClusterShardingSettings(system).withRole(shardingRole),
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
