package rros.play

import akka.actor.{PoisonPill, Props, Actor, ActorRef}
import rros.Socket
////////////////////////////////////////////////////////////////////////////////
/**
 * Use in play framework
 * {{{
  def socket = WebSocket.acceptWithActor[String,String] {request => out => 
    val adapter = PlaySocketAdapter(out)
    val protocol = RROSProtocolImpl(adapter) //save this somewhere 
    protocol.onRequestReceived { implicit RROSRequest => 
      //do something 
      Response("OK","Hello world")
    }
    PlaySocketAdapter(out).handler
  }
 * }}} 
 * Created by namnguyen on 3/3/15.
 */
case class PlaySocketAdapter(outActor:ActorRef) extends Socket{
  class InActor(out:ActorRef,socket: Socket) extends Actor {
    override def receive = {
      case msg:String => {
        socket.socketListeners.map(_.onReceived(msg))
      }
    }
    override def postStop() = {
      socket.socketListeners.map(_.onClose())
    }
  }
  //----------------------------------------------------------------------------
  override def send(message: String): Unit = {
    outActor ! message
  }
  //----------------------------------------------------------------------------
  override def close(): Unit = {
    outActor ! PoisonPill
  }
  //----------------------------------------------------------------------------
  lazy val handler = Props(new InActor(outActor,this))
  //----------------------------------------------------------------------------
}
////////////////////////////////////////////////////////////////////////////////