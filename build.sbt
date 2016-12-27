name := "ThreePhaseCommit"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.14",
  "com.typesafe.akka" %% "akka-remote" % "2.4.14"
)

scalacOptions ++= Seq("-feature")

lazy val misraProject = RootProject(uri("git://github.com/Artii15/Misra83.git"))
lazy val root = Project("root", file(".")) dependsOn misraProject