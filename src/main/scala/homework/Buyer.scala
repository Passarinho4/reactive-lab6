package homework

import java.util.Random

import akka.actor.Actor
import akka.event.LoggingReceive
import homework.Buyer._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Buyer(val id: Int, auctionSearchPath: String) extends Actor {

  val random = new Random()
  var bidCounter = 0

  val auctionSearch = context.actorSelection(auctionSearchPath)

  override def receive: Receive = LoggingReceive {
    case Init =>
      val title = AuctionService.auctionTitles(random.nextInt(AuctionService.auctionTitles.length))
      //val title = AuctionService.auctionTitles.head
      auctionSearch ! AuctionSearch.SearchAuction(title)

    case msg:AuctionSearch.SearchResult =>
      if(msg.auctions.nonEmpty) {
        msg.auctions(random.nextInt(msg.auctions.length)) ! Auction.Bid(random.nextInt(100), this.self)
        this.context.system.scheduler.scheduleOnce(20 seconds, this.self, Bid)
      }

    case Bid if bidCounter < 4 =>
      bidCounter = bidCounter + 1
      val title = AuctionService.auctionTitles(random.nextInt(AuctionService.auctionTitles.length))
      //val title = AuctionService.auctionTitles.head
      auctionSearch ! AuctionSearch.SearchAuction(title)

    case Bid if bidCounter >=4 =>

    case Auction.YouWonTheAuction(price) =>
      println("Hurra! I won the auction")


  }

  override def toString: String = "Buyer $id"
}

object Buyer {

  def apply(id: Int, auctionSeatchPath: String): Buyer = new Buyer(id, auctionSeatchPath)

  case object Init
  case object Bid

}
