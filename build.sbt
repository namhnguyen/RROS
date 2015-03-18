name := "rros-protocol"

organization := "rros"

version := "1.0"

scalaVersion := "2.11.6"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

assemblyJarName in assembly := "rros.jar"

mainClass in assembly := Some("rros.App")

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.9"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.10"

libraryDependencies += "org.eclipse.jetty.websocket" % "websocket-client" % "9.2.9.v20150224"

libraryDependencies += "javax.websocket" % "javax.websocket-client-api" % "1.1"

libraryDependencies += "org.glassfish.tyrus" % "tyrus-client" % "1.10"

libraryDependencies += "org.glassfish.tyrus" % "tyrus-server" % "1.10"

libraryDependencies += "org.glassfish.tyrus" % "tyrus-container-grizzly" % "1.2.1"

libraryDependencies += "com.typesafe.play" %% "play-iteratees" % "2.3.8"
//libraryDependencies += "org.eclipse.jetty" % "jetty-websocket" % "8.1.16.v20140903"


//libraryDependencies += "com.typesafe.play" %% "play" % "2.3.8"
//only for testing client - lots of bugs
//libraryDependencies += "io.backchat.hookup" %% "hookup" % "0.3.0"

