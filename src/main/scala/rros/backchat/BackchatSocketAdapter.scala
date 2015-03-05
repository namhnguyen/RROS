package rros.backchat

import scala.concurrent.ExecutionContext.Implicits.global
import rros.Socket
import io.backchat.hookup._
import org.json4s.jackson.JsonMethods._
/**
 * Created by namnguyen on 3/4/15.
 */
case class BackchatSocketAdapter(config:HookupClientConfig) extends Socket{
  implicit val defaultFormats = org.json4s.DefaultFormats
  val client = new DefaultHookupClient(config){
    def receive = {
      case Disconnected(_) =>
        println("The websocket to " + config.getUri().toASCIIString + " disconnected.")
        socketListeners.map(_.onClose())
      case TextMessage(message) => {
//        println("RECV: " + message)
//        socketListeners.map(_.onReceived(message))
        socketListeners.map(_.onReceived(message))
        //send("ECHO: " + message)
      }
      case r:JsonMessage => {
        val str = compact(render(r.content))
        socketListeners.map(_.onReceived(str))
      }
    }

    connect() onSuccess {
      case Success =>
          println("The websocket is connected to:" + config.getUri().toASCIIString + ".")
      case _ => {
        socketListeners.map(_.onClose())
      }
    }
  }
  //----------------------------------------------------------------------------
  override def send(message: String): Unit = {
    val result = client.send(message)
  }
  //----------------------------------------------------------------------------
  override def close(): Unit = client.close()
  //----------------------------------------------------------------------------
}
