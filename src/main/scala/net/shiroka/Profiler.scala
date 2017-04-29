package net.shiroka
import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import java.io._
import java.util.Date
import java.text.SimpleDateFormat
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

class Profiler extends Actor {
  import Profiler._

  val system = context.system
  val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
  implicit val ec: ExecutionContext = context.dispatcher

  override def preStart: Unit = {
    super.preStart
    system.scheduler.schedule(2.seconds, interval, self, Profile)
  }

  def receive = {
    case Profile => profile
  }

  def profile: Unit = {
    val rt = Runtime.getRuntime
    val line = Seq(now, rt.totalMemory, rt.freeMemory, rt.maxMemory).mkString("\t")
    if (stdout) {
      println(line)
    } else {
      val out = new PrintWriter(new FileOutputStream(new File(filename), true))
      try { out.println(line) }
      finally { out.close() }
    }
  }

  def now = df.format(new Date())
}

object Profiler {
  val config = ConfigFactory.load.getConfig("net.shiroka.profiler")
  val interval = config.as[FiniteDuration]("interval")
  val stdout = config.as[Boolean]("stdout")
  val filename = "log/profile.log"

  object Profile

  def run(system: ActorSystem): Unit = {
    system.actorOf(props)
  }

  def props = Props(classOf[Profiler])
}
