package rros.core

import akka.actor.Props
import rros._
import rros.core.RROSActorSystem._

////////////////////////////////////////////////////////////////////////////////
/**
 * Created by namnguyen on 3/2/15.
 */
class RROSProtocolImpl(socket:Socket) extends RROSProtocol with SocketListener{
  socket += this
  //create akka actor
  val managementActorRef = RROSActorSystem.system.actorOf(Props(classOf[ManagementActor],socket,this))

  //----------------------------------------------------------------------------
  override def send(request: Request, onComplete: (Response) => Unit, timeOut: Long, onFailure: (Exception) => Unit): Unit = {
    managementActorRef ! SendRequest(request,onComplete,timeOut,onFailure)
  }
  //----------------------------------------------------------------------------
  override def send(message: Message): Unit = {
    managementActorRef ! SendMessage(message)
  }

  //----------------------------------------------------------------------------
  override def onMessageReceived(callback:Option[(Message) => Unit]): Unit =
    _messageReceivedCallback = callback
  //----------------------------------------------------------------------------
  override def onRequestReceived(callback:Option[(Request) => Response]): Unit =
    _requestReceivedCallback = callback
  //----------------------------------------------------------------------------
  override def onReceived(message: String): Unit = {
    managementActorRef ! OnSocketMessageReceived(message)
  }
  //----------------------------------------------------------------------------
  override def onClose(): Unit = {
    this.close()
  }
  //----------------------------------------------------------------------------
  override def onFailure(exc:Exception): Unit = {
    this.close()
  }
  //----------------------------------------------------------------------------
  /**
   * Close the sub-protocol Session. This will unregister the listener from
   * the Socket
   */
  override def close(): Unit = {
    socket -= this
    RROSActorSystem.system.stop(managementActorRef)
  }
  //----------------------------------------------------------------------------
  def messageReceivedCallback = _messageReceivedCallback
  def requestReceivedCallback = _requestReceivedCallback
  //----------------------------------------------------------------------------
  private var _messageReceivedCallback:Option[(Message)=>Unit] = None
  private var _requestReceivedCallback:Option[(Request)=>Response] = None
  //----------------------------------------------------------------------------

}
////////////////////////////////////////////////////////////////////////////////