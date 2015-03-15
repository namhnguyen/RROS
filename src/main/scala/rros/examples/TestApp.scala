package rros.examples

import java.util.concurrent.CountDownLatch

import rros.{Response, Request, RROSProtocol, SocketListener}
import rros.core.{RROSActorSystem, RROSProtocolImpl}

/**
 * Created by namnguyen on 3/2/15.
 */
object TestApp {
  //----------------------------------------------------------------------------
  def main(args:Array[String]): Unit ={
    testRROSOverMemorySockets()

  }


  //----------------------------------------------------------------------------
  def testRROSOverMemorySockets(): Unit ={
    for (loop <- 1 to 1)
    {
      val adapterEndPoint1 = MemorySocketAdapter("Socket 1")
      val adapterEndPoint2 = MemorySocketAdapter("Socket 2")
      adapterEndPoint1.otherSocket = adapterEndPoint2
      adapterEndPoint2.otherSocket = adapterEndPoint1
      val session1 = RROSProtocol(adapterEndPoint1)
      val session2 = RROSProtocol(adapterEndPoint2)
      session2.onRequestReceived(Some { implicit msg =>
        Thread.sleep(10)
        Response("Ok", Some("Session 2 response to :" + msg.body))
      })
      session1.onRequestReceived(Some { implicit msg =>
        //println("Receive: "+msg.body)
        Thread.sleep(100)
        Response("Ok", Some("Session 1 response to :" + msg.body))
      })
      for (i <- 1 to 100) {
        println(s"Session 1 send [$i]")
        session1.send(Request("Ask", "testuri", Some(s"hello from $i"))
          , onComplete = { implicit response => println(s"session 1 asks item $i: " + response)}
          , timeOut = 10000
          , onFailure = { implicit exc => println(exc)}
        )
        Thread.sleep(1)
      }

      for (i <- 1 to 100) {
        println(s"Session 2 send [$i]")
        //val countDown = new CountDownLatch(1)
        session2.send(Request("Ask", "testuri", Some(s"hello from $i"))
          , onComplete = { implicit response =>
            println(s"session 2 asks item $i: " + response)
          //  countDown.countDown()
          }
          , timeOut = 10000
          , onFailure = { implicit exc => println(exc)
          //  countDown.countDown()
          }
        )
        //countDown.await()
        //Thread.sleep(1)
      }

      Thread.sleep(10000)
      adapterEndPoint1.close()
      adapterEndPoint2.close()
    }

    RROSActorSystem.system.shutdown()
  }
  //----------------------------------------------------------------------------
  def testMemorySocket(): Unit ={
    println("Test Socket Memory")
    val socket1 = MemorySocketAdapter("Socket 1")
    val socket2 = MemorySocketAdapter("Socket 2")
    socket1.otherSocket = socket2
    socket2.otherSocket = socket1
    socket1 += new SocketListener {
      override def onClose(): Unit = ???

      override def onFailure(exception: Exception): Unit = ???

      override def onReceived(message: String): Unit = println("Socket 1 received: "+message)

      override def onConnect(): Unit = ???
    }
    socket2 += new SocketListener {
      override def onClose(): Unit = ???

      override def onFailure(exception: Exception): Unit = ???

      override def onReceived(message: String): Unit = println("Socket 2 received: "+message)

      override def onConnect(): Unit = ???
    }
    socket1.send("Hi I am 1")
    socket2.send("Hi I am 2")
  }
  //----------------------------------------------------------------------------
}
