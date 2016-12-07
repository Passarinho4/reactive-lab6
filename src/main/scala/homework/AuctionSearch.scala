package homework

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import akka.routing._
import homework.AuctionSearch.{AddAuction, SearchAuction, SearchResult}

class AuctionSearch(id: String) extends Actor {

  var map:Map[String, ActorRef] = Map()

  override def receive: Receive = LoggingReceive {
    case msg:AddAuction =>
      map = map + ((msg.title, msg.auction))
      println(s"Broadcast add auction passed to AuctionSearch $id, title: ${msg.title}")
    case msg: SearchAuction =>
      println(s"Search Auction Message handled by $id")
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
  val routees = (1 to 5).map( i => {
    val r = context.actorOf(Props(new AuctionSearch(String.valueOf(i))))
    context.watch(r)
    ActorRefRoutee(r)
  })

  var router = {
    Router(RoundRobinRoutingLogic(), routees)
  }
  var broadcastRouter = {
    Router(BroadcastRoutingLogic(), routees)
  }

  override def receive: Receive = LoggingReceive {
    case msg:SearchAuction => router.route(msg, sender())
    case Broadcast(msg:AddAuction) => broadcastRouter.route(msg, sender())
  }
}