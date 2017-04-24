package net.shiroka
import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import java.io._
import java.util.Date
import java.text.SimpleDateFormat

class Profiler extends Actor {
  import Profiler._

  val system = context.system
  val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
  implicit val ec: ExecutionContext = context.dispatcher

  override def preStart: Unit = {
    super.preStart
    system.scheduler.schedule(2.seconds, interval.seconds, self, Profile)
  }

  def receive = {
    case Profile => profile
  }

  def profile: Unit = {
    val rt = Runtime.getRuntime
    val row = Seq(now, rt.totalMemory, rt.freeMemory, rt.maxMemory)
    val out = new PrintWriter(new FileOutputStream(new File(filename), true))
    try { out.println(row.mkString("\t")) }
    finally { out.close() }
  }

  def now = df.format(new Date())
}

object Profiler {
  object Profile
  val filename = "log/profile.log"
  val interval = 10
  def run(system: ActorSystem): Unit = {
    system.actorOf(props)
  }

  def props = Props(classOf[Profiler])
}
