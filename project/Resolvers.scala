import sbt._

object Resolvers {
  val ivyLocal = Resolver.file("local-ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))
}
