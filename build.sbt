import sbt._
import Settings._

val `app` = Project("ev-charging", file("."))
  .settings(commonSettings)
  .settings(organization := "com.anzop")
  .settings(name := "app-backend")
  .settings(version := "0.0.1")
  .settings(libraryDependencies ++= Dependencies.libraryDependencies)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

enablePlugins(JavaAppPackaging)
