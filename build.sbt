lazy val scala3_5_0 = "3.5.0"
lazy val scala2_13_14 = "2.13.14"
lazy val scala2_12_19 = "2.12.19"
lazy val supportedScalaVersions = Vector(scala3_5_0, scala2_13_14, scala2_12_19)

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "gg.sina"
ThisBuild / scalaVersion := scala3_5_0
ThisBuild / crossScalaVersions := supportedScalaVersions

lazy val root = (project in file("."))
  .settings(
    name := "agent",
  )

libraryDependencies += "org.apache.pekko" %% "pekko-actor-typed" % "1.1.0"