package rros.examples

import rros.{SocketListener, SocketAdapter}

/**
 * Created by namnguyen on 3/2/15.
 */


case class MemorySocketAdapter(name:String) extends SocketAdapter {
  var otherSocket:SocketAdapter = null
  //----------------------------------------------------------------------------
  override def send(message: String): Unit = {
    otherSocket.socketListeners.map(_.onReceived(message))
  }
  //----------------------------------------------------------------------------
  override def close(): Unit = {
    this.socketListeners.map(_.close())
  }
}