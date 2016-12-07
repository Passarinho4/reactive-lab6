package homework

import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSelection}
import akka.persistence.fsm.PersistentFSM
import homework.Auction._

import scala.concurrent.duration._
import scala.reflect._

class Auction(val id: String,
              notifier: ActorSelection,
              bidTime: FiniteDuration,
              deleteTime: FiniteDuration) extends PersistentFSM[State, Data, UpdateStateEvent]{

  def persistenceId = "persistent-auction-fsm-id-" + id
  override def domainEventClassTag: ClassTag[UpdateStateEvent] = classTag[UpdateStateEvent]

  startWith(Created, AuctionData(0, None, new Date(), 0 milliseconds))
  setTimer(bidTimer, BidTimerExpired, bidTime)

  def countAuctionDuration(a: AuctionData): FiniteDuration = {
    a.auctionDuration + FiniteDuration(new Date().getTime - a.lastEventTime.getTime, TimeUnit.MILLISECONDS)
  }

  when(Created) {
    case Event(BidTimerExpired, a: AuctionData) => {
      setTimer(deleteTimer, DeleteTimerExpired, deleteTime)
      goto(Ignored) applying UpdateStateEvent(a.buyer, a.price, new Date(), countAuctionDuration(a))
    }
    case Event(bid: Bid, a:AuctionData) => {
      if(bid.price > a.price) {
        notifier ! Notifier.Notify(id, bid.buyer, bid.price)
        goto(Activated) applying UpdateStateEvent(Some(bid.buyer), bid.price, new Date(), countAuctionDuration(a))
      } else {
        stay()
      }
    }
  }

  when(Ignored) {
    case Event(DeleteTimerExpired, a:AuctionData) => {
      println("Auction stopped without buyer")
      stop()
    }
    case Event(Relist, a:AuctionData) => {
      cancelTimer(deleteTimer)
      goto(Created)
    }
  }

  when(Activated) {
    case Event(bid: Bid, a: AuctionData) =>
        goto(Activated) applying UpdateStateEvent(Some(bid.buyer), bid.price, new Date(), countAuctionDuration(a))
    case Event(BidTimerExpired, a:AuctionData) =>
      a.buyer.get ! YouWonTheAuction(a.price)
      context.parent ! Auction.AuctionFinished(id, a.price, a.buyer.get)
      setTimer(deleteTimer, DeleteTimerExpired, deleteTime)
      goto(Sold) applying UpdateStateEvent(a.buyer, a.price, new Date(), countAuctionDuration(a))
  }

  when(Sold) {
    case Event(DeleteTimerExpired, a:AuctionData) =>
      println("Stopping auction " + id)
      stop()
  }

  override def applyEvent(event: UpdateStateEvent, dataBeforeEvent: Data):Data = (event, dataBeforeEvent) match {
    case (event:UpdateStateEvent, data: AuctionData) => {
      println(s"Handling UpdateStateEvent [old price:${data.price} new:${event.price}]," +
        s"lastEventTime: ${data.lastEventTime.toString} eventTime: ${event.eventTime.toString}" +
        s" auctionTime = ${event.auctionDuration.toString()}")
      if (event.price > data.price) {
        if(event.buyer.isDefined){
          notifier ! Notifier.Notify(id, event.buyer.get, event.price)
        }
        AuctionData(event.price, event.buyer, event.eventTime, event.auctionDuration)
      } else {
        AuctionData(data.price, data.buyer, event.eventTime, event.auctionDuration)
      }
    }
  }

  private def checkBidTimer(data: AuctionData) = {
    stateName match {
      case (Created | Activated) => {
        val duration = bidTime - data.auctionDuration
        setTimer(bidTimer, BidTimerExpired, duration)
      }
      case _ =>
    }
  }

  private def checkDeleteTimer(data: AuctionData) = {
    stateName match {
      case (Ignored | Sold) => {
        if(!isTimerActive(deleteTimer)) {
          val duration = deleteTime - (data.auctionDuration - bidTime)
          setTimer(deleteTimer, DeleteTimerExpired, duration)
        }
      }
      case _ =>
    }
  }

  override def onRecoveryCompleted(): Unit = {
    this.stateData match {
      case data:AuctionData => {
        //it's a hack, because I don't want to create special Actor for counting time
        //more info here https://groups.google.com/forum/#!msg/akka-user/-BgGMB63br4/HtJikMggAQAJ
        data.lastEventTime = new Date()
        println(s"Hacking system: lastEventTime: ${data.lastEventTime.toString}, auctionDuration: ${data.auctionDuration.toString()}")
        checkBidTimer(data)
        checkDeleteTimer(data)
      }
    }
    println("Recovery Completed!")
  }

}

object Auction {

  def apply(id: String, notifier: ActorSelection, bidTime: FiniteDuration, deleteTime: FiniteDuration): Auction =
    new Auction(id, notifier, bidTime, deleteTime)

  val bidTimer: String = "bidTimer"
  val deleteTimer: String = "deleteTimer"

  case object BidTimerExpired
  case object DeleteTimerExpired
  case object Relist
  case class Bid(price: BigDecimal, buyer: ActorRef)
  case class YouWonTheAuction(price: BigDecimal)
  case class AuctionFinished(title: String, price: BigDecimal, buyer: ActorRef)


  sealed trait State extends PersistentFSM.FSMState

  case object Created extends State {
    override def identifier: String = "Created"
  }
  case object Ignored extends State {
    override def identifier: String = "Ignored"
  }
  case object Activated extends State {
    override def identifier: String = "Activated"
  }
  case object Sold extends State {
    override def identifier: String = "Sold"
  }

  sealed trait Data
  final case class AuctionData(price: BigDecimal, buyer: Option[ActorRef],
                               var lastEventTime: Date, auctionDuration: FiniteDuration) extends Data

  case class UpdateStateEvent(buyer: Option[ActorRef], price: BigDecimal, eventTime: Date, auctionDuration: FiniteDuration)
}