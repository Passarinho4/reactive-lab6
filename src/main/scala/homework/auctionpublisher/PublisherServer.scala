package homework.auctionpublisher

import akka.actor.Status.Success
import akka.actor.{Actor, ActorRef}
import akka.event.LoggingReceive
import homework.auctionpublisher.PublisherServer.PublisherNotification

class PublisherServer extends Actor {

  override def receive: Receive = LoggingReceive {
    case msg:PublisherNotification =>
      println(s"[PS]: Received notification (title:${msg.title}, buyer:${msg.buyer}, price: ${msg.price})")
      sender() ! Success
  }

}

object PublisherServer {
  case class PublisherNotification(title: String, buyer: ActorRef, price: BigDecimal)
}
