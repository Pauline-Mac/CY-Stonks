ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

lazy val root = (project in file("."))
  .settings(
    name := "Cy_Stonks"
  )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.8.0",
  "com.typesafe.akka" %% "akka-stream" % "2.8.0",
  "org.postgresql" % "postgresql" % "42.5.1"
)

libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.7"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.11"

