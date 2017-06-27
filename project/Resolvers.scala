import sbt._

object Resolvers {
  val ivyLocal = Resolver.file("local-ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))
  val tanukkii007 = Resolver.bintrayRepo("tanukkii007", "maven")
}
