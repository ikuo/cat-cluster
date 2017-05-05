package net.shiroka.cat
import scala.concurrent.duration._
import akka.actor._
import akka.testkit._
import akka.cluster.sharding._
import org.specs2.mutable._
import org.specs2.specification.AfterAll
import ShardRegion._

class ProfilerSpec extends TestKit(ActorSystem("profiler-spec"))
    with ImplicitSender with SpecificationLike with AfterAll {
  import Profiler._

  def afterAll = TestKit.shutdownActorSystem(system)

  "Profiler" >> {
    "#receive" >> {
      "with ClusterShardingStats" >> {
        "it sends stats with redis memory to an emitter" in {
          val profiler = system.actorOf(Props(classOf[Profiler]))
          val emitter = TestProbe("emitter")
          profiler ! Emitter(Some(emitter.ref))
          profiler ! ClusterShardingStats(Map.empty)
          emitter.expectMsgClass(2.seconds, classOf[Stats])
          ok
        }
      }
    }
  }
}
