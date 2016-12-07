package homework2.homework

import akka.actor.{Actor, ActorRef, ActorSelection}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import homework.Notifier.Notify

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class Notifier extends Actor {
  implicit val timeout = Timeout(5 seconds)

  private val publisherServer: ActorSelection = context.actorSelection("akka.tcp://AuctionPublisherSystem@127.0.0.1:2556/user/publisherServer")

  override def receive: Receive = LoggingReceive {
    case msg:Notify =>
      val result: Future[Any] = publisherServer ? auctionpublisher.PublisherServer.PublisherNotification(msg.title, msg.buyer, msg.price)
      result.onSuccess { case _ => println("[Notifier] Successfully published notification.") }
      result.onFailure { case x:Throwable => println(s"[Notifier] Error while publishing notification: ${x.getMessage}")}
  }

}

object Notifier {
  case class Notify(title: String, buyer: ActorRef, price: BigDecimal)
}
