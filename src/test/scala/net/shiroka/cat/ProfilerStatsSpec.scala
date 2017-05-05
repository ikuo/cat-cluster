package net.shiroka.cat
import akka.cluster.sharding._
import org.specs2.mutable._
import org.specs2.specification.Scope
import ShardRegion._

class ProfilerStatsSpec extends SpecificationLike {
  "Profiler.Stats" >> {
    "#apply" >> {
      "with redis info string" >> {
        "it builds redis info map" in {
          val src = """# Memory
used_memory:501208
used_memory_human:489.46K
used_memory_rss:7446528
used_memory_peak:501128
used_memory_peak_human:489.38K
used_memory_lua:33792
mem_fragmentation_ratio:14.86
mem_allocator:jemalloc-3.4.1
"""
          val stats = Profiler.Stats(ClusterShardingStats(Map.empty), src)
          stats.redis.get("used_memory") must beSome("501208")
        }
      }
    }
  }
}
