name := "play_socket_server"

version := "1.0"

lazy val `play_socket_server` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq( jdbc , anorm , cache , ws )

libraryDependencies += "rros" %% "rros-protocol" % "1.0"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.3.9"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  