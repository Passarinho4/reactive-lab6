package homework2.homework

import java.util.Random

import akka.actor.Actor
import akka.event.LoggingReceive
import homework2.homework.Buyer.{Bid, Init}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Buyer(val id: Int, auctionSearchPath: String) extends Actor {

  val random = new Random()
  var bidCounter = 0

  var counter = 0
  val searchNumber = 10000
  var start: Long = _
  var stop: Long = _

  val auctionSearch = context.actorSelection(auctionSearchPath)

  override def receive: Receive =  {
    case Init =>
      println("RECEIVED START MESSAGE")
      start = System.nanoTime()
      (0 to searchNumber).foreach(i => {
        val title = AuctionService.auctionTitles(random.nextInt(AuctionService.auctionTitles.length))
        auctionSearch ! AuctionSearch.SearchAuction(title)
      })

    case msg:AuctionSearch.SearchResult =>
      counter = counter + 1
      if(counter % 1000 == 0) {
        println(s"Counter: $counter")
      }
      if(counter == searchNumber) {
        stop = System.nanoTime()
        println(s"Test result in seconds = ${(stop - start)/1000000000L}")
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
