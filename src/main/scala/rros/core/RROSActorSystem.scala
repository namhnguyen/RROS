package rros.core

import akka.actor.{Props, Actor, ActorSystem}

import scala.concurrent._

////////////////////////////////////////////////////////////////////////////////
/**
 *
 * Created by namnguyen on 3/2/15.
 */
object RROSActorSystem {
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
        blocking {
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
  case class Reminder()
  private case class SelfLoop()
  //----------------------------------------------------------------------------
}
////////////////////////////////////////////////////////////////////////////////