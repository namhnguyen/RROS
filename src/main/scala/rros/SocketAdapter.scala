package rros
////////////////////////////////////////////////////////////////////////////////
/**
 * In order for other socket to use the RROS protocol, developer needs to implement
 * a small adapter which implement the SocketInterface.
 * Created by namnguyen on 3/1/15.
 */
trait SocketAdapter extends AutoCloseable{
  //----------------------------------------------------------------------------
  def send(message:String):Unit
  //----------------------------------------------------------------------------
  def close():Unit
  //----------------------------------------------------------------------------
  def += (socketListener: SocketListener):Unit =
    this.synchronized {
      _socketListeners = _socketListeners + socketListener
    }
  //----------------------------------------------------------------------------
  def -= (socketListener: SocketListener):Unit = 
    this.synchronized {
      _socketListeners = _socketListeners - socketListener
    }
  //----------------------------------------------------------------------------
  def socketListeners = _socketListeners
  //----------------------------------------------------------------------------
  private var _socketListeners:Set[SocketListener] = Set()
  //----------------------------------------------------------------------------
}
////////////////////////////////////////////////////////////////////////////////