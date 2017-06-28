package ru.tinkoff

import akka.actor.{Actor, ActorRef, Props}

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

object Publisher {
  case object Publish

  def props(subscriber: ActorRef, period: FiniteDuration, messageSize: Int, messagesCount: Int): Props = Props(
    new Publisher(subscriber, period, messageSize, messagesCount)
  )
}

class Publisher(subscriber: ActorRef, period: FiniteDuration, messageSize: Int, messagesCount: Int) extends Actor {
  import Publisher._
  import context.dispatcher

  private val message = (1 to messageSize).map(_ => Random.nextPrintableChar()).mkString

  override def preStart(): Unit = {
    context.system.scheduler.schedule(period, period, self, Publish)
  }

  override def receive: Receive = {
    case Publish => (1 to messagesCount).foreach(_ => subscriber ! message)
  }
}