package rros.play

import akka.actor._
import play.api.libs.iteratee.{Iteratee, Concurrent}
import play.api.libs.iteratee.Concurrent.Channel
import rros.SocketAdapter
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by namnguyen on 3/11/15.
 */
class ReceiverSocketAdapter(
                             fromEndPointId:String
                             ,channelManagementTable: ChannelManagementTable)(implicit remoteActorSystem:ActorSystem) extends SocketAdapter{

  var managementActorRef:Option[ActorRef] = None

  val in = Iteratee.foreach[String] ( str => {
    //trigger all socket listeners onReceived
    this.socketListeners.map(_.onReceived(str))
    //broadcast received message to all Other ListenerActor (on other servers)
    managementActorRef.map(_ ! MessageReceived(str))
  }).map(_=>{
    this.socketListeners.map(_.onClose())
    //notify other listener that I am closed
    val path = this.getActorServerPath(remoteActorSystem,managementActorRef.get)
    channelManagementTable.unregisterIfExists(fromEndPointId,path)
    managementActorRef.map(_ ! WebSocketClose)
    managementActorRef.map(_ ! PoisonPill )
    //only unregister if have not been overwritten

  })

  var out_channel:Option[Channel[String]] = None

  val out = Concurrent.unicast[String](
    onStart = { implicit channel =>
      //val computerName= InetAddress.getLocalHost.getHostName
      //println(computerName)
      managementActorRef = Some(remoteActorSystem
        .actorOf(Props(new ManagementActor(this))))
      val oldActorPath = channelManagementTable.get(endPointId = fromEndPointId)
      val newActorPath = this.getActorServerPath(remoteActorSystem,managementActorRef.get)
      //println(newActorPath)

      channelManagementTable.register(
        endPointId = fromEndPointId,
        newActorPath
      )

      //forceClose other actor that is using the same endPointId
      if (oldActorPath.isDefined)
        remoteActorSystem.actorSelection(oldActorPath.get) ! ForceClose
      out_channel = Some(channel)
    }
    ,onError = { case (reason,inputStr) =>
      this.socketListeners.map(_.onFailure(new Exception(reason)))
    }
  )
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
  override def send(message: String): Unit = out_channel.map(_.push(message))

  override def close(): Unit = out_channel.map(_.eofAndEnd())

  def handle = (in,out)



  /**
   * ManagementActor is used to communicate across different servers and thus
   * needs to be remote host
   */
  class ManagementActor(socketAdapter: SocketAdapter) extends Actor {
    val inboundListeners = scala.collection.mutable.HashSet[ActorRef]()
    override def receive: Receive = {
      case RegisterToReceiveMessage(otherManagementActor) =>
        inboundListeners += otherManagementActor
      case UnregisterToReceiveMessage(otherManagementActor) =>
        inboundListeners -= otherManagementActor
      case msg:MessageReceived => //broadcast to all listeners on other servers
        inboundListeners.map(_ ! msg)
      case SendMessage(message) => socketAdapter.send(message) //from other actor
      case WebSocketClose =>
        inboundListeners.map(_ ! WebSocketClose)
      case ForceClose =>
        socketAdapter.close()
    }
  }
}

