name := "RROS"

version := "1.0"

scalaVersion := "2.11.5"

assemblyJarName in assembly := "rros.jar"

mainClass in assembly := Some("rros.App")

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.9"

libraryDependencies += "net.liftweb" %% "lift-json" % "3.0-M3"