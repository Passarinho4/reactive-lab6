package homework2.homework

import akka.actor.{Actor, ActorRef, Props}
import akka.routing._
import homework2.homework.AuctionSearch.{AddAuction, SearchAuction, SearchResult}

import scala.concurrent.duration.FiniteDuration

class AuctionSearch(id: String) extends Actor {

  var map:Map[String, ActorRef] = Map()

  override def receive: Receive = {
    case msg:AddAuction =>
      map = map + ((msg.title, msg.auction))
      if(map.size % 1000 == 0) {
        println(s"Map size: ${map.size}")
      }
      //println(s"Broadcast add auction passed to AuctionSearch $id, title: ${msg.title}")
    case msg: SearchAuction =>
      //println(s"Search Auction Message handled by $id")
      val list: List[ActorRef] = map.filterKeys(_.contains(msg.query)).values.toList
      sender() ! SearchResult(msg.query, list)

  }
}

object AuctionSearch {

  case class AddAuction(title: String, auction:ActorRef)
  case class SearchAuction(query: String)
  case class SearchResult(query: String, auctions: List[ActorRef])

}

class MasterSearch extends Actor {
  import scala.concurrent.duration._

  val routees = (1 to 5).map( i => {
    val r = context.actorOf(Props(new AuctionSearch(String.valueOf(i))))
    context.watch(r)
    ActorRefRoutee(r)
  })

  var router = {

    Router(ScatterGatherFirstCompletedRoutingLogic(10 seconds), routees)
  }
  var broadcastRouter = {
    Router(BroadcastRoutingLogic(), routees)
  }

  override def receive: Receive =  {
    case msg:SearchAuction => router.route(msg, sender())
    case Broadcast(msg:AddAuction) => broadcastRouter.route(msg, sender())
  }
}