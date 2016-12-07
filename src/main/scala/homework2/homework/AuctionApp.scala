package homework2.homework

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import homework.auctionpublisher.PublisherServer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AuctionApp extends App {
  val config = ConfigFactory.load()

  val auctionSystem = ActorSystem("AuctionSystem", config.getConfig("auctionsystem")
    .withFallback(config))
  val auctionPublisherSystem = ActorSystem("AuctionPublisherSystem", config.getConfig("auctionpublisher")
    .withFallback(config))

  private val actor: ActorRef = auctionPublisherSystem.actorOf(Props[PublisherServer], "publisherServer")
  println(s"Publisher server path: ${actor.path}")


  val mainActor = auctionSystem.actorOf(Props[AuctionService], "mainActor")

  mainActor ! AuctionService.Init

  Await.result(auctionSystem.whenTerminated, Duration.Inf)
}
