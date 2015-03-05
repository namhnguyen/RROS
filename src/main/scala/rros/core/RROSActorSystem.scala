package rros.core

import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import JsonUtils._
import com.typesafe.config.ConfigFactory
import org.json4s._
import rros._
import scala.concurrent._

////////////////////////////////////////////////////////////////////////////////
/**
 *
 * Created by namnguyen on 3/2/15.
 */
object RROSActorSystem {
  private val customConf = ConfigFactory.parseString(
    """
      |akka {
      | actor {
      |   provider = "akka.actor.LocalActorRefProvider"
      | }
      |}
    """.stripMargin)
  //----------------------------------------------------------------------------
  private val _system = ActorSystem("RROSActorSystem",
    ConfigFactory.load(customConf).withFallback(ConfigFactory.defaultOverrides()))
  private val _timerActor = _system.actorOf(Props[TimerActor])
  _timerActor ! SelfLoop
  //----------------------------------------------------------------------------
  def shutdown(): Unit ={
    _system.stop(_timerActor)
    Thread.sleep(100)
    _system.shutdown()
  }
  //----------------------------------------------------------------------------
  def system = _system
  //----------------------------------------------------------------------------
  class TimerActor extends Actor {
    var prev:Long = System.currentTimeMillis()
    override def receive = {
      case SelfLoop => {
        blocking { // in case running out of thread pool
          Thread.sleep(1)
        }
        val cur = System.currentTimeMillis()
        val delta = cur-prev
        if (delta>1000) {
          prev = cur
          val all  = context.actorSelection("/user/*")
          all ! Reminder
        }else {
          self ! SelfLoop
        }
      }
      case _ => {
        self ! SelfLoop
      }
    }
  }
  //----------------------------------------------------------------------------
  class ManagementActor(socketAdapter:SocketAdapter,rROSSession: RROSProtocolImpl) extends Actor {
    val callbackActorRef = context.actorOf(Props[CallbackActor])
    val networkActorRef = context.actorOf(Props(classOf[NetworkActor], socketAdapter))
    private val maxAwaitingSentRequests = 100
    //private val maxAwaitingProcessedRequests = 100
    private val sentTable = scala.collection.mutable.HashMap[String, SentRecord]()
    private val receivedTable = scala.collection.mutable.HashMap[String, ReceivedRecord]()

    private case class SentRecord(id: String, request: Request
                                  , onComplete: (Response) => Unit
                                  , onFailure: (Exception) => Unit
                                  , sentTime: Long, timeOut: Long)

    private case class ReceivedRecord(requestPackage: RequestPackage
                                      , receivedTime: Long, timeOut: Long, workerRef: ActorRef)

    //--------------------------------------------------------------------------
    override def receive = {
      case r: SendRequest => {
        if (sentTable.size < maxAwaitingSentRequests) {
          val key = GUID.randomGUID
          sentTable +=
            (key -> SentRecord(key, r.request, r.onComplete, r.onFailure, System.currentTimeMillis(), r.timeOut))
          networkActorRef ! RequestPackage(key, r.request.verb, r.request.uri, r.request.body, r.timeOut)
          sender ! SentRequestAccepted
        } else {
          //callbackActorRef ! ExecuteFailureCallback(r.onFailure, new MaxAwaitingRequestException())
          sender ! new MaxAwaitingRequestException()
        }
      }
      case r: SendMessage => {
        networkActorRef ! MessagePackage(r.message.value)
      }
      case OnSocketMessageReceived(value) => {
        //try to parse the message to json
        try {
          val jValue = deserialize(value)
          if (jValue \ "id" != JNothing && jValue \ "verb" != JNothing && jValue \ "uri" != JNothing) {
            val requestPackage = jValue.extract[RequestPackage]
            //store into reived request
            if (rROSSession.requestReceivedCallback.isDefined) {
              val workerRef = context.actorOf(Props(classOf[WorkerActor], rROSSession))
              val receivedRecord = ReceivedRecord(requestPackage, System.currentTimeMillis(), requestPackage.timeout, workerRef)
              workerRef ! ProcessRequest(requestPackage)
              receivedTable += (requestPackage.id -> receivedRecord)
            } else {
              networkActorRef ! ResponsePackage(requestPackage.id, ResponseCode.NoRequestHandlerOnOtherSide, None)
            }

          } else if (jValue \ "id" != JNothing && jValue \ "code" != JNothing) {
            val responsePackage = jValue.extract[ResponsePackage]
            //marshal back to response
            val requestId = responsePackage.id
            if (sentTable.contains(requestId)) {
              val response = Response(responsePackage.code, responsePackage.response)
              callbackActorRef ! ExecuteOkCallback(sentTable.get(requestId).get.onComplete, response)
              sentTable -= (requestId)
            } else {
              //do nothing
            }
          } else if (jValue \ "value" != JNothing) {
            val messagePackage = jValue.extract[MessagePackage]
            val workerRef = context.actorOf(Props(classOf[WorkerActor], rROSSession))
            workerRef ! ProcessMessage(messagePackage)
          } else {
            //invalid message
            println("Invalid JSON format")
          }

        } catch {
          case e: Exception => {
            //log invalid message
            println("Invalid Message: "+e.getMessage)
          }
        }

      }
      case CompleteResponse(requestId, r) => {
        if (receivedTable.contains(requestId)) {
          networkActorRef ! ResponsePackage(requestId, r.code, r.response)
          receivedTable -= (requestId)
        }
      }
      case Reminder => {
        //check sent/received request tables, and remove timeout request
        //check sent
        val curTime = System.currentTimeMillis()
        val timeOutSentRequests =
          sentTable.filter { case (k, v) => (v.sentTime + v.timeOut) < curTime}
        sentTable --= timeOutSentRequests.map { case (k, v) => k}
        timeOutSentRequests.map { case (k, v) =>
          callbackActorRef ! ExecuteFailureCallback(v.onFailure, new RequestTimeoutException())
        } //local

        val timeOutProcessedRequests =
          receivedTable.filter { case (k, v) => (v.receivedTime + v.timeOut) < curTime}
        receivedTable --= timeOutProcessedRequests.map { case (k, v) => k}
        timeOutProcessedRequests.map { case (k, v) => {
          networkActorRef ! ResponsePackage(k, ResponseCode.ResponseTimeout, None)
          context.stop(v.workerRef)
          }
        }

      }
    }
  }
  //--------------------------------------------------------------------------

  /**
   * handle sending message over network
   */
  class NetworkActor(socketAdapter:SocketAdapter) extends Actor {
    override def receive = {
      case r: RequestPackage => {
        val json = serialize(r)
        socketAdapter.send(json)
      }
      case r: ResponsePackage => {
        val json = serialize(r)
        socketAdapter.send(json)
      }
      case r: MessagePackage => {
        val json = serialize(r)
        socketAdapter.send(json)
      }
    }
  }
  //----------------------------------------------------------------------------
  /**
   * Created by ManagementActor to perform callback
   */
  class CallbackActor extends Actor {
    override def receive = {
      case ExecuteOkCallback(callback,r) => callback.apply(r)
      case ExecuteFailureCallback(callback,e) => callback.apply(e)
    }
  }
  //----------------------------------------------------------------------------
  /**
   * Created by ManagementActor to handle request
   */
  class WorkerActor(rROSSession:RROSProtocolImpl) extends Actor {
    override def receive = {
      case ProcessRequest(requestPackage)=>{
        //Marshal back to Request
        val request = Request(requestPackage.verb, requestPackage.uri, requestPackage.body)
        val response = rROSSession.requestReceivedCallback.get.apply(request)
        context.parent ! CompleteResponse(requestPackage.id,response)
        context.stop(self)
      }
      case ProcessMessage(messagePackage)=> {
        val message = Message(messagePackage.value)
        rROSSession.messageReceivedCallback.get.apply(message)
        context.stop(self)
      }
    }
  }
  //----------------------------------------------------------------------------
  object SentRequestAccepted
  case class SendRequest(request:Request
                         , onComplete: (Response) => Unit
                         , timeOut: Long
                         , onFailure: (Exception) => Unit)
  case class SendMessage(message: Message)
  case class OnSocketMessageReceived(message:String)
  case class ExecuteOkCallback(callback:(Response)=>Unit,response:Response) //used by CallbackActor
  case class ExecuteFailureCallback(callback:(Exception)=>Unit,exception:Exception) //used by CallbackActor
  case class ProcessRequest(requestPackage: RequestPackage) //used by WorkerActor
  case class ProcessMessage(messagePackage:MessagePackage) //used by worker actor
  case class CompleteResponse(requestId:String,response: Response) //workerActor send to its parent
  private object Reminder
  private object SelfLoop
  //----------------------------------------------------------------------------
}
////////////////////////////////////////////////////////////////////////////////