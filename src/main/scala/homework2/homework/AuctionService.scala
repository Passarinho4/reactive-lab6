package homework2.homework

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class AuctionService extends Actor {

  import AuctionService._

  import scala.concurrent.duration._

  var buyer:ActorRef = _

  override def receive: Receive = LoggingReceive {
    case Init =>

      val auctionSearchName: String = "auctionSearch"
      val auctionSearchMaster = context.system.actorOf(Props(new MasterSearch()), auctionSearchName)
      auctionSearchMaster.path

      val notifierName: String = "notifierName"
      val notifier = context.system.actorOf(Props(new Notifier()), notifierName)


      context.system.actorOf(Props(new Seller(1,
        s"../$auctionSearchName",
        s"../$notifierName",
        auctionTitles,
        this.self)))


      buyer = context.system.actorOf(Props(Buyer(1, s"../$auctionSearchName")))


    case StartBuyer => {
      buyer ! Buyer.Init
    }

  }

}

object AuctionService {

  val auctionTitles: List[String] = (1 to 50000).map(String.valueOf).toList

  case object Init

  case object StartBuyer

}