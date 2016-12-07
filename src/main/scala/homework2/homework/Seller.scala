package homework2.homework

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import akka.routing.Broadcast
import homework2.homework.AuctionSearch.AddAuction

import scala.concurrent.duration._
import scala.language.postfixOps

class Seller(val id: Int,
             auctionSearchPath:String,
             notifierPath: String,
             auctionTitles: List[String],
             auctionService: ActorRef) extends Actor {

  val auctionSearch = context.actorSelection(auctionSearchPath)
  val notifier = context.actorSelection(notifierPath)

  for { auctionTitle <- auctionTitles } {
    val auction = context.actorOf(Props(Auction(auctionTitle, notifier, 100 seconds, 100 seconds)))
    auctionSearch ! Broadcast(AddAuction(auctionTitle, auction))
  }

  auctionService ! AuctionService.StartBuyer


  override def receive: Receive = LoggingReceive {

    case msg:Auction.AuctionFinished => {
      println(s"I sell ${msg.title} price ${msg.price} buyer ${msg.buyer}")
    }
  }
}
