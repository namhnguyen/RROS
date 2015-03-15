package controllers

import java.util.concurrent.CountDownLatch

import play.api.mvc._
import play.libs.Akka
import rros.{SocketListener, Response, RROSProtocol}
import rros.play.{PublisherSocketAdapter, ChannelManagementTableImpl, ReceiverSocketAdapter}
import scala.util.Random

/**
 * Created by namnguyen on 3/12/15.
 */
object Sockets extends Controller{

  implicit val remoteActorSystem = Akka.system()
  val managementTable = new ChannelManagementTableImpl()

  /**
   * Create a bi-directional channel
   * @param endpoint
   * @return
   */
  def socket(endpoint:String) = WebSocket.using[String] { request =>
    val rrosAdapter = new ReceiverSocketAdapter(endpoint, managementTable)
    rrosAdapter += new SocketListener {
      override def onClose(): Unit = { println(s"$endpoint close")}

      override def onFailure(exc: Exception): Unit = { println(s"$endpoint failure")}

      override def onReceived(message: String): Unit = { println(s"$endpoint recceive $message")}

      override def onConnect(): Unit = { println("On Connect")}
    }
    val rros_protocol = RROSProtocol(rrosAdapter)
    rros_protocol.onRequestReceived( Some { implicit rros_request =>
      println(rros_request)
      Thread.sleep(Random.nextInt(500))
      Response("OK",Some("request ["+rros_request.uri+"]"))
    } )
    rrosAdapter.handle
  }

  /**
   * Push from http request
   * @param endpoint
   * @param content
   * @return
   */
  def push(endpoint:String,verb:String,resource:String,content:String) = Action { request =>
    val pushAdapter = new PublisherSocketAdapter(endpoint,managementTable)
    val rros_protocol = RROSProtocol(pushAdapter)
    val countDown = new CountDownLatch(1)
    var out_response:rros.Response = null
    var out_exception:Exception = null
    rros_protocol.send(
      rros.Request(verb,resource,Some(content))
      ,onComplete = { response =>
        out_response = response
        countDown.countDown()
      }
      ,onFailure = { exc =>
        out_exception = exc
        countDown.countDown()
      }
    )
    countDown.await()
    pushAdapter.close()
    if (out_response!=null)
      Ok(s"$out_response")
    else
      Ok(s"$out_exception")
  }
}
