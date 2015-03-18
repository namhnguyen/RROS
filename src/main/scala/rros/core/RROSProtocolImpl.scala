package rros.core

import akka.actor.Props
import rros._
import rros.core.RROSActorSystem._
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.{AskTimeoutException, ask}



////////////////////////////////////////////////////////////////////////////////
/**
 * Created by namnguyen on 3/2/15.
 */
class RROSProtocolImpl(socketAdapter:SocketAdapter) extends RROSProtocol with SocketListener{
  //create akka actor
  val managementActorRef = RROSActorSystem.system.actorOf(Props(classOf[ManagementActor],socketAdapter,this))
  private val RETRY_MAX_COUNT = 10
  private val RETRY_DELAY = 10
  private val RETRY_DELAY_PUSHBACK_FACTOR = 2 //next wait will be twice as slower 
  private implicit val askTimeout = Timeout(5 seconds)
  //remember to attach the socketAdapter after everything is initialized
  socketAdapter += this
  //----------------------------------------------------------------------------
  override def send(request: Request
                    , onComplete: (Response) => Unit
                    , timeOut: Long
                    , onFailure: (Exception) => Unit): Unit = {
    var retryCount:Int = 0
    var needRetry:Boolean = false
    var retryDelay:Long = RETRY_DELAY
    do {
      try {
        val f = managementActorRef ? SendRequest(request, onComplete, timeOut, onFailure)
        val result = Await.result(f, 2 seconds) //this always suppose to be fast
        result match {
          case SentRequestAccepted => {
            needRetry = false
            retryCount = 0
            retryDelay = RETRY_DELAY
          }
          case exc: MaxAwaitingRequestException => {
            needRetry = true
            retryCount = retryCount + 1
            retryDelay = retryDelay * RETRY_DELAY_PUSHBACK_FACTOR
            if (retryCount < RETRY_MAX_COUNT) {
              Thread.sleep(retryDelay)
            } else {
              onFailure(exc)
            }
          }
        }
      } catch {
        case exc:AskTimeoutException => {
          needRetry = true
          retryCount = retryCount + 1
          retryDelay = retryDelay * RETRY_DELAY_PUSHBACK_FACTOR
          if (retryCount < RETRY_MAX_COUNT) {
            Thread.sleep(retryDelay)
          } else {
            onFailure(exc)
          }
        }
      }
    }while(needRetry && retryCount < RETRY_MAX_COUNT)
  }
  override def send(rawString: String): Unit = managementActorRef ! SendString(rawString)
  //----------------------------------------------------------------------------

  //----------------------------------------------------------------------------
  override def send(message: Message): Unit = {
    managementActorRef ! SendMessage(message)
  }
  //----------------------------------------------------------------------------
  override def onConnect():Unit = { }
  //----------------------------------------------------------------------------
  override def onMessageReceived(callback:Option[(Message) => Unit]): Unit =
    _messageReceivedCallback = callback
  //----------------------------------------------------------------------------
  override def onRequestReceived(callback:Option[(Request) => Response]): Unit =
    _requestReceivedCallback = callback
  //----------------------------------------------------------------------------
  override def onAnythingReceived(callback: Option[(String) => Unit]): Unit =
    _anythingReceivedCallback = callback
  //----------------------------------------------------------------------------
  override def onReceived(message: String): Unit =
    managementActorRef ! OnSocketMessageReceived(message)
  //----------------------------------------------------------------------------
  override def onClose(): Unit = {
    this.close()
  }
  //----------------------------------------------------------------------------
  override def onFailure(exc:Exception): Unit = {
    //log exception
    this.close()
  }
  //----------------------------------------------------------------------------
  /**
   * Close the sub-protocol Session. This will unregister the listener from
   * the Socket
   */
  override def close(): Unit = {
    socketAdapter -= this
    RROSActorSystem.system.stop(managementActorRef)
  }
  //----------------------------------------------------------------------------
  def messageReceivedCallback = _messageReceivedCallback
  def requestReceivedCallback = _requestReceivedCallback
  def anythingReceivedCallback = _anythingReceivedCallback
  //----------------------------------------------------------------------------
  private var _messageReceivedCallback:Option[(Message)=>Unit] = None
  private var _requestReceivedCallback:Option[(Request)=>Response] = None
  private var _anythingReceivedCallback:Option[(String)=>Unit] = None
  //----------------------------------------------------------------------------
}
////////////////////////////////////////////////////////////////////////////////