package rros.examples

import rros.{SocketListener, Socket}

/**
 * Created by namnguyen on 3/2/15.
 */


case class MemorySocket(name:String) extends Socket {
  var otherSocket:Socket = null
  //----------------------------------------------------------------------------
  override def send(message: String): Unit = {
    otherSocket.socketListeners.map(_.onReceived(message))
  }
  //----------------------------------------------------------------------------
  override def close(): Unit = {
    this.socketListeners.map(_.close())
  }
}