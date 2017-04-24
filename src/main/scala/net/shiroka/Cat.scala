package net.shiroka

import akka.actor._

class Cat extends Actor {
  import Cat._

  private var numMeow = 0

  def receive = {
    case msg: Meow => this.numMeow += 1
  }
}

object Cat {
  case class Meow(catId: String)
  case class GetMeows(catId: String)
}
