import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "net.shiroka",
      scalaVersion := "2.12.1",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "cat-cluster",
    libraryDependencies ++= Seq(
      akka("cluster"),
      akka("cluster-sharding"),
      ficus,
      specs2 % Test
    ),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )
