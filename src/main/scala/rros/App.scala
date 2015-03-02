package rros

import rros.core.RROSActorSystem
import rros.core.RROSActorSystem.Reminder

import scala.concurrent.blocking
import akka.actor.{Props, Actor, ActorSystem}

/**
 * Created by namnguyen on 3/1/15.
 */
object App {
  //----------------------------------------------------------------------------
  def main(args:Array[String]): Unit = {
    println("Project RROS...")
//    val ref1 = RROSActorSystem.system.actorOf(Props(new FirstActor("Actor 1")))
//    val ref2 = RROSActorSystem.system.actorOf(Props(new FirstActor("Actor 2")))
//    ref1 ! "hello world 1"
//    ref2 ! "hello world 2"
//    Thread.sleep(2000)
//    ref2 ! "kill"
//    Thread.sleep(10000)
//    RROSActorSystem.system.shutdown()
    //testActorSystem()
  }
//  //----------------------------------------------------------------------------
//  val actorSystem = ActorSystem()
//  def testActorSystem(): Unit ={
//    val firstRef = actorSystem.actorOf(Props(new FirstActor("First")))
//    val secondRef = actorSystem.actorOf(Props(new FirstActor("Second")))
//    val timer = actorSystem.actorOf(Props[TimerActor])
//    firstRef ! "Hello world"
//    firstRef ! "new"
//    timer ! SelfLoop()
//    //val all  = actorSystem.actorSelection("/user/*") //children wont be selected
//    //all ! "hello world again"
//    Thread.sleep(10000)
//    actorSystem.shutdown()
//  }
//  //----------------------------------------------------------------------------
//  class FirstActor(name:String) extends Actor {
//    override def receive = {
//      case msg:String if (msg!="kill") => println(s"Parent [$name]: "+msg)
//      case msg:String if (msg=="kill") => {
////        val child = context.actorOf(Props[ChildActor])
////        child ! "hello world"
//        context.stop(self)
//      }
//      case _:Reminder => println(s"[$name]: I am reminded")
//    }
//  }
//  class ChildActor extends Actor {
//    override def receive = {
//      case msg:String => println("Child: "+ msg)
//    }
//  }
//  class TimerActor extends Actor {
//    var count:Int = 0;
//    override def receive = {
//      case _:SelfLoop => {
//        blocking {
//          Thread.sleep(1)
//        }
//        count = count + 1
//        if (count>1000) {
//          count = 0
//          val all  = context.actorSelection("/user/*")
//          all ! Reminder()
//        }
//        self ! SelfLoop()
//      }
//      case _ => {}
//    }
//  }
//
//  case class Reminder()
//  case class SelfLoop()
}
