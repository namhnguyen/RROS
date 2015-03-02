package rros.core

import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import net.liftweb.json.JsonAST.JNothing
import rros._
import net.liftweb.json.Serialization.write
import scala.concurrent._

////////////////////////////////////////////////////////////////////////////////
/**
 *
 * Created by namnguyen on 3/2/15.
 */
object RROSActorSystem {
  implicit val jsonFormat = net.liftweb.json.DefaultFormats
  //----------------------------------------------------------------------------
  private val _system = ActorSystem("RROSActorSystem")
  private val _timerActor = _system.actorOf(Props[TimerActor])
  _timerActor ! SelfLoop()
  //----------------------------------------------------------------------------
  def system = _system
  //----------------------------------------------------------------------------
  class TimerActor extends Actor {
    var prev:Long = System.currentTimeMillis()
    override def receive = {
      case _:SelfLoop => {
        blocking { // in case running out of thread pool
          Thread.sleep(1)
        }
        val cur = System.currentTimeMillis()
        val delta = cur-prev
        if (delta>1000) {
          prev = cur
          val all  = context.actorSelection("/user/*")
          all ! Reminder()
        }else {
          self ! SelfLoop()
        }
      }
      case _ => {
        self ! SelfLoop()
      }
    }
  }
  //----------------------------------------------------------------------------
  class ManagementActor(socket:Socket,rROSSession: RROSSessionImpl) extends Actor {
    val callbackActorRef = context.actorOf(Props[CallbackActor])
    val networkActorRef = context.actorOf(Props(classOf[NetworkActor],socket))
    private val maxAwaitingSentRequests = 100
    //private val maxAwaitingProcessedRequests = 100
    private val sentTable = scala.collection.mutable.HashMap[String,SentRecord]()
    private val receivedTable = scala.collection.mutable.HashMap[String,ReceivedRecord]()

    private case class SentRecord(id:String,request:Request
                                  ,onComplete:(Response)=>Unit
                                  ,onFailure:(Exception)=>Unit
                                  ,sentTime:Long,timeOut:Long)
    private case class ReceivedRecord(requestPackage: RequestPackage
                                      ,receivedTime:Long,timeOut:Long,workerRef:ActorRef)
    //--------------------------------------------------------------------------
    override def receive = {
      case r:SendRequest => {
        if (sentTable.size < maxAwaitingSentRequests){
          val key = GUID.randomGUID
          sentTable +=
            (key -> SentRecord(key,r.request,r.onComplete,r.onFailure,System.currentTimeMillis(),r.timeOut))
          networkActorRef ! RequestPackage(key,r.request.verb,r.request.uri,r.request.body,r.timeOut)
        } else {
          callbackActorRef ! ExecuteFailureCallback(r.onFailure,new MaxAwaitingRequestException())
        }
      }
      case r:SendMessage => {
        networkActorRef ! MessagePackage(r.message.value)
      }
      case OnSocketMessageReceived(value) => {
        //try to parse the message to json
        try {
          val jValue = net.liftweb.json.parse(value)
          if (jValue \ "id" != JNothing && jValue \ "verb" != JNothing && jValue \ "uri" != JNothing){
            val requestPackage = jValue.extract[RequestPackage]
            //store into reived request
            if (rROSSession.requestReceivedCallback.isDefined) {
              val workerRef = context.actorOf(Props(classOf[WorkerActor], rROSSession))
              val receivedRecord = ReceivedRecord(requestPackage,System.currentTimeMillis(),requestPackage.timeout,workerRef)
              workerRef ! ProcessRequest(requestPackage)
              receivedTable += (requestPackage.id -> receivedRecord)
            } else {
              networkActorRef ! ResponsePackage(requestPackage.id,ResponseCode.NoRequestHandlerOnOtherSide,None)
            }

          } else if (jValue \ "id"!=JNothing && jValue \"code" != JNothing){
            val responsePackage = jValue.extract[ResponsePackage]
            //marshal back to response
            val requestId = responsePackage.id
            if (sentTable.contains(requestId)){
              val response = Response(responsePackage.code,responsePackage.response)
              callbackActorRef ! ExecuteOkCallback(sentTable.get(requestId).get.onComplete,response)
              sentTable -= (requestId)
            } else {
              //do nothing
            }
          } else if (jValue \ "value"!=JNothing){
            val messagePackage= jValue.extract[MessagePackage]
            val workerRef = context.actorOf(Props(classOf[WorkerActor], rROSSession))
            workerRef ! ProcessMessage(messagePackage)
          } else {
            //invalid message
            println("Invalid JSON format")
          }

        }catch{
          case e:Exception => {
            //log invalid message
            println(e.getMessage)
          }
        }

      }
      case CompleteResponse(requestId,r) => {
        if (receivedTable.contains(requestId)){
          networkActorRef ! ResponsePackage(requestId,r.code,r.response)
          receivedTable -= (requestId)
        }
      }
      case _:Reminder =>{
        //check sent/received request tables, and remove timeout request
        //check sent
        val curTime = System.currentTimeMillis()
        val timeOutSentRequests =
          sentTable.filter { case (k,v)=> (v.sentTime+v.timeOut)<curTime}
        sentTable --= timeOutSentRequests.map { case (k,v) => k }
        timeOutSentRequests.map { case (k,v) =>
                callbackActorRef ! ExecuteFailureCallback(v.onFailure, new RequestTimeoutException()) } //local

        val timeOutProcessedRequests =
          receivedTable.filter { case(k,v) => (v.receivedTime+v.timeOut)<curTime}
        receivedTable --= timeOutProcessedRequests.map { case (k,v)=>k }
        timeOutProcessedRequests.map { case (k, v) => {
            networkActorRef ! ResponsePackage(k, ResponseCode.ResponseTimeout, None)
            context.stop(v.workerRef)
          }
        }

      }
    }
    //--------------------------------------------------------------------------

  }
  //----------------------------------------------------------------------------
  /**
   * handle sending message over network
   */
  class NetworkActor(socket:Socket) extends Actor {
    override def receive = {
      case r: RequestPackage => {
        val json = write(r)
        socket.send(json)
      }
      case r: ResponsePackage => {
        val json = write(r)
        socket.send(json)
      }
      case r: MessagePackage => {
        val json = write(r)
        socket.send(json)
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
  class WorkerActor(rROSSession:RROSSessionImpl) extends Actor {
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
  private case class Reminder()
  private case class SelfLoop()
  //----------------------------------------------------------------------------
}
////////////////////////////////////////////////////////////////////////////////