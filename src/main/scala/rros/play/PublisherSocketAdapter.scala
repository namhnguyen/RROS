package rros.play

import akka.actor._
import rros.SocketAdapter
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
/**
 *
 * Used by the server to push request to any client
 *
 * Created by namnguyen on 3/11/15.
 */
class PublisherSocketAdapter(
    endPointId:String
    ,channelManagementTable: ChannelManagementTable)
  (implicit remoteActorSystem:ActorSystem) extends SocketAdapter {

  //get endPointId from channelManagementTable
  //create its own Actor and send the ref to the receiver

  val listenerActorRef = remoteActorSystem.actorOf(
    Props(new ReceiverListenerActor(this,endPointId,channelManagementTable)))
  listenerActorRef ! CheckReceiverActor
  this.socketListeners.map(_.onConnect())
  //  val cancellable = Akka.system.scheduler.schedule(
  //     0 milliseconds
  //    ,10000 milliseconds
  //    ,listenerActorRef
  //    ,CheckReceiverActor
  //  )
  override def send(message: String): Unit =
    listenerActorRef ! SendMessage(message)

  override def close(): Unit = listenerActorRef ! PoisonPill

  class ReceiverListenerActor(
                               socketAdapter: SocketAdapter
                               ,endPointId:String
                               ,managementTable: ChannelManagementTable) extends Actor {

    private var someListeningActorPath:Option[String] = None

    private def updateListeningActorPath = {
      val oldValue = someListeningActorPath

      val newValue = managementTable.get(endPointId)
      if (newValue != oldValue) {
        if (oldValue.isDefined) {
          remoteActorSystem.actorSelection(oldValue.get) ! UnregisterToReceiveMessage(self)
        }
        if (newValue.isDefined) {
          remoteActorSystem.actorSelection(newValue.get) ! RegisterToReceiveMessage(self)
        }
      }
      someListeningActorPath = newValue
      newValue
    }
    override def receive = {
      case CheckReceiverActor => updateListeningActorPath

      case MessageReceived(m) => {
        socketAdapter.socketListeners.map(_.onReceived(m))
      }

      case command:SendMessage => {
        someListeningActorPath.map(path => {
          val target = remoteActorSystem.actorSelection(path)
          val f = target.resolveOne(Duration(5,SECONDS))
          f.map( actorRef => {
            if (actorRef!=null) {
              actorRef ! command
            }
            else {
              //update path and retry
              updateListeningActorPath.map(
                newPath => remoteActorSystem.actorSelection(newPath) ! command)
            }
          })
        })
      }

      case WebSocketClose => {
        self ! CheckReceiverActor
      }
    }
    override def postStop() = {
      //cancellable.cancel()
      socketAdapter.socketListeners.map(_.onClose())
      someListeningActorPath.map(path=>
        remoteActorSystem.actorSelection(path) ! UnregisterToReceiveMessage(self))
    }
  }
}
