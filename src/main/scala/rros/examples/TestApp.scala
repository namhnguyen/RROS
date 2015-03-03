package rros.examples

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
    for (loop <- 1 to 50)
    {
      val socket1 = MemorySocket("Socket 1")
      val socket2 = MemorySocket("Socket 2")
      socket1.otherSocket = socket2
      socket2.otherSocket = socket1
      val session1: RROSProtocol = new RROSProtocolImpl(socket1)
      val session2: RROSProtocol = new RROSProtocolImpl(socket2)
      session2.onRequestReceived(Some { implicit msg =>
        Thread.sleep(1000)
        Response("Ok", Some("Session 2 response to :" + msg.body))
      })
      session1.onRequestReceived(Some { implicit msg =>
        Thread.sleep(1000)
        Response("Ok", Some("Session 1 response to :" + msg.body))
      })
      for (i <- 1 to 10) {
        session1.send(Request("Ask", "testuri", Some(s"hello from $i"))
          , onComplete = { implicit response => println(s"session 1 asks item $i: " + response)}
          , timeOut = 2000
          , onFailure = { implicit exc => println(exc)}
        )
      }

      for (i <- 1 to 10) {
        session2.send(Request("Ask", "testuri", Some(s"hello from $i"))
          , onComplete = { implicit response => println(s"session 2 asks item $i: " + response)}
          , timeOut = 2000
          , onFailure = { implicit exc => println(exc)}
        )
      }

      Thread.sleep(2000)
      socket1.close()
      socket2.close()
    }



    RROSActorSystem.system.shutdown()
  }
  //----------------------------------------------------------------------------
  def testMemorySocket(): Unit ={
    println("Test Socket Memory")
    val socket1 = MemorySocket("Socket 1")
    val socket2 = MemorySocket("Socket 2")
    socket1.otherSocket = socket2
    socket2.otherSocket = socket1
    socket1 += new SocketListener {
      override def onClose(): Unit = ???

      override def onFailure(exception: Exception): Unit = ???

      override def onReceived(message: String): Unit = println("Socket 1 received: "+message)

      override def close(): Unit = ???
    }
    socket2 += new SocketListener {override def onClose(): Unit = ???

      override def onFailure(exception: Exception): Unit = ???

      override def onReceived(message: String): Unit = println("Socket 2 received: "+message)

      override def close(): Unit = ???
    }
    socket1.send("Hi I am 1")
    socket2.send("Hi I am 2")
  }
  //----------------------------------------------------------------------------
}
