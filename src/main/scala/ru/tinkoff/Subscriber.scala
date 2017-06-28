package ru.tinkoff

import akka.actor._
import scala.concurrent.duration._

object Subscriber {
  case object Stats

  def props(): Props = Props(new Subscriber())
}

class Subscriber extends Actor with ActorLogging {
  import Subscriber._
  import context.dispatcher

  private var count = 0L

  override def preStart(): Unit = {
    context.system.scheduler.schedule(1.second, 1.second, self, Stats)
  }

  override def receive: Receive = {
    case Stats =>
      log.info("messages/second: {}", count)
      count = 0

    case _ =>
      count += 1
  }
}
