package rros.play

/**
 * Created by namnguyen on 3/12/15.
 */
import akka.actor.ActorRef

/**
 * Created by namnguyen on 3/5/15.
 */
case class RegisterToReceiveMessage(actor:ActorRef)
case class UnregisterToReceiveMessage(actor:ActorRef)
case class MessageReceived(message:String)
case class SendMessage(message:String)
object ForceClose
object WebSocketClose
object CheckReceiverActor