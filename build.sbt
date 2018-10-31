import Dependencies._

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.perhac.experiments",
      scalaVersion := "2.12.7",
      version := "0.1.0-SNAPSHOT"
    )
  ),
  name := "akka-streams tinkering",
  libraryDependencies ++= Seq(cats, akka, (scalaTest % Test))
)

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Ypartial-unification")
