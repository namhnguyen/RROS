package rros
////////////////////////////////////////////////////////////////////////////////
/**
 * Request/Response over Socket Session 
 *
 * Created by namnguyen on 3/1/15.
 */
trait RROSProtocol extends AutoCloseable{
  //----------------------------------------------------------------------------
  def send(request: Request
           ,onComplete:(Response)=>Unit
           ,timeOut:Long = 30000 //30 seconds
           ,onFailure:(Exception) => Unit = (exc:Exception) => {
              //default log here
            }
  ):Unit
  //----------------------------------------------------------------------------
  def send(message:Message):Unit
  //----------------------------------------------------------------------------
  def onMessageReceived(callback:Option[(Message)=>Unit]):Unit
  def onRequestReceived(callback:Option[(Request)=>Response]):Unit
  //----------------------------------------------------------------------------
}
////////////////////////////////////////////////////////////////////////////////