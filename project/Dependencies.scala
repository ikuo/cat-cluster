import sbt._

object Dependencies {
  lazy val specs2 = "org.specs2" %% "specs2-core" % "3.8.9"
  def akka(module: String) = "com.typesafe.akka" %% s"akka-$module" % "2.5.0"
}
