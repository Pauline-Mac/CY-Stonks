ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

Compile / run / mainClass := Some("Main")


lazy val root = (project in file("."))
  .settings(
    name := "CY_Stonks"
  )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.8.3",
  "com.typesafe.akka" %% "akka-http" % "10.5.2",
  "org.json4s" %% "json4s-native" % "4.0.3",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.2",
  "com.typesafe.akka" %% "akka-stream" % "2.8.3",
  "org.postgresql" % "postgresql" % "42.5.1",
  "org.mindrot" % "jbcrypt" % "0.4",
  "com.lihaoyi" %% "requests" % "0.8.0"
)

libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.7"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.11"

