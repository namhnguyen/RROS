package rros.play

import akka.actor.{ActorRef, ActorSystem}

/**
 * Created by namnguyen on 4/5/15.
 */
object ActorUtils {

  def getActorServerPath(remoteActorSystem:ActorSystem, actorRef:ActorRef):String = {
    val config = remoteActorSystem.settings.config
      .getConfig("akka")
      .getConfig("remote").getConfig("netty.tcp")
    val hostname = config.getString("hostname")
    val port = config.getString("port")
    val value = actorRef.path.toString
      .replace("://application", s".tcp://application@$hostname:$port")
    //println(value)
    value
  }

}
