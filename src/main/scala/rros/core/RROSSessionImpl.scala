package rros.core

import rros._
////////////////////////////////////////////////////////////////////////////////
/**
 * Created by namnguyen on 3/2/15.
 */
class RROSSessionImpl(socket:Socket) extends RROSSession with SocketListener{
  socket += this
  //create akka actor

  //----------------------------------------------------------------------------
  override def send(request: Request, onComplete: (Response) => Unit, timeOut: Long, onFailure: (Exception) => Unit): Unit = ???

  //----------------------------------------------------------------------------
  override def send(message: Message): Unit = ???

  //----------------------------------------------------------------------------
  override def onMessageReceived(callback:Option[(Message) => Unit]): Unit =
    _messageReceivedCallback = callback
  //----------------------------------------------------------------------------
  override def onRequestReceived(callback:Option[(Request) => Response]): Unit =
    _requestReceivedCallback = callback
  //----------------------------------------------------------------------------
  override def onReceived(message: String): Unit = ???
  //----------------------------------------------------------------------------
  override def onClose(): Unit = ???
  //----------------------------------------------------------------------------
  override def onFailure(): Unit = ???
  //----------------------------------------------------------------------------
  /**
   * Close the sub-protocol Session. This will unregister the listener from
   * the Socket
   */
  override def close(): Unit = {
    socket -= this
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