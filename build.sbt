import sbt.Keys._
import sbt._
import Dependencies._
import Resolvers._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "net.shiroka",
      scalaVersion := "2.12.1",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "cat-cluster",
    resolvers ++= Seq(ivyLocal, tanukkii007),
    libraryDependencies ++= Seq(
      akka("cluster"),
      akka("cluster-sharding"),
      akkaRedis,
      ficus,
      splitBrainResolver,
      akka("testkit") % Test,
      specs2 % Test
    ),
    scalacOptions in Test ++= Seq("-Yrangepos"),
    fork in Test := true,
    test in assembly := {},
    PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)
  )
