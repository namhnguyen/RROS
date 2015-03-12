import java.net.URI

import rros.{Request, RROSProtocol, Response}

/**
 * Created by namnguyen on 3/12/15.
 */
object App {
  def main(args:Array[String]): Unit ={
    println("Test Socket...")
    val clientId = "macbookpro15"
    val socketAdapter = new
        rros.java.adapters.JettyWebSocketClientAdapter(
          new URI(s"ws://localhost:9000/sockets/rros/$clientId"))
    socketAdapter.connect()

    val protocol = RROSProtocol(socketAdapter.toScalaSocketAdapter)
    //when receive push from server
    protocol.onRequestReceived(callback = Some({ implicit  request =>
      println(request)
      Response("OK",Some(s"From Client [$clientId] content [$request]"))
    }))

    //try to send infinite request to server
    var i:Int = 1
    while(true){
      val cur = i
      protocol.send(Request("POST",s"c://test/$clientId/$cur",Some(s"TestBody [$clientId/$cur]"))
        ,onComplete = { implicit response =>
          println(s"Response from server: $response")
        }
        ,onFailure = { implicit exception =>
          println(s"Exception: $exception")
        }
      )
      i = i + 1
      Thread.sleep(1000)
    }
    //wait forever
    Thread.sleep(1000000)
  }
}
