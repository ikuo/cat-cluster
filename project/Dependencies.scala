import sbt._

object Dependencies {
  lazy val specs2 = "org.specs2" %% "specs2-core" % "3.8.9"
  lazy val ficus = "com.iheart" %% "ficus" % "1.4.0"
  lazy val akkaRedis = "com.safety-data" %% "akka-persistence-redis" % "0.2.0-SNAPSHOT"
  lazy val splitBrainResolver = "com.github.TanUkkii007" %% "akka-cluster-custom-downing" % "0.0.7"
  def akka(module: String) = "com.typesafe.akka" %% s"akka-$module" % "2.5.0"
}
