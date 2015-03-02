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
  override def onMessageReceived(callback: (Message) => Unit): Unit = ???
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
}
////////////////////////////////////////////////////////////////////////////////