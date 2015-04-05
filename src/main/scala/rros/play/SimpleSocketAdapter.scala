package rros.play

import akka.actor._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Concurrent, Iteratee}
import rros.{GlobalConfig, SocketAdapter}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by namnguyen on 4/5/15.
 */
class SimpleSocketAdapter(topic:String,topicSubscriberTable: TopicSubscriberTable)
    (implicit remoteActorSystem:ActorSystem) extends SocketAdapter{

  val PING_TIME_OUT: Long = GlobalConfig.PING_TIME_OUT
  val PING_DURATION: Long = GlobalConfig.PING_DURATION
  var managementActorRef:Option[ActorRef] = None
  var lastReceivedMessageTime:Long = System.currentTimeMillis()
  var lastPing:Long = System.currentTimeMillis()
  var cancellableTimer:Option[Cancellable] = None

  val in = Iteratee.foreach[String] ( str => {
    //trigger all socket listeners onReceived
    lastReceivedMessageTime = System.currentTimeMillis()
    if (str!="\0") {
      this.socketListeners.map(_.onReceived(str))
      //broadcast received message to all Other ListenerActor (on other servers)
      managementActorRef.map(_ ! MessageReceived(str))
    }
  }).map(_=>{
    this.socketListeners.map(_.onClose())
    //notify other listener that I am closed
    val path = ActorUtils.getActorServerPath(remoteActorSystem,managementActorRef.get)
    topicSubscriberTable.unsubscribe(topic,path)
    cancellableTimer.map(_.cancel())
//  managementActorRef.map(_ ! WebSocketClose)
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

      val newActorPath = ActorUtils.getActorServerPath(remoteActorSystem,managementActorRef.get)
      //println(newActorPath)

      topicSubscriberTable.subscribe(
        topic = topic
        ,actorPath = newActorPath
      )

      out_channel = Some(channel)

      this.socketListeners.map(_.onConnect())

      cancellableTimer = Some(rros.core.RROSActorSystem.system.scheduler
        .schedule(FiniteDuration(0,SECONDS),FiniteDuration(1,SECONDS))
      {
        managementActorRef.get ! Ping
      })
    }
    ,onError = { case (reason,inputStr) =>
      this.socketListeners.map(_.onFailure(new Exception(reason)))
    }
  )
  //----------------------------------------------------------------------------
  def handle = (in,out)
  //----------------------------------------------------------------------------
  override def send(message: String): Unit = out_channel.map(_.push(message))

  //----------------------------------------------------------------------------
  override def close(): Unit = out_channel.map(_.eofAndEnd())

  class ManagementActor(socketAdapter: SimpleSocketAdapter) extends Actor {
    override def receive: Receive = {
      case Ping => {
        val now = System.currentTimeMillis()
        val durationFromLastPing = now - socketAdapter.lastPing
        if (durationFromLastPing > PING_DURATION) {
          socketAdapter.lastPing = now
          socketAdapter.send("\0")
        }
      }
      case msg:String => {
        socketAdapter.send(msg)
      }

    }
  }
}
