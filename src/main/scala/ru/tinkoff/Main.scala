package ru.tinkoff

import akka.actor._
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Future
import scala.concurrent.duration._

object Main {

  lazy val role: ApplicationRole = System.getenv("ROLE") match {
    case "publisher" => ApplicationRoles.Publisher
    case "subscriber" => ApplicationRoles.Subscriber
    case _ => throw new Exception("Invalid application role. Only [publisher/subscriber] allowed")
  }

  lazy val mode: ApplicationMode = System.getenv("MODE") match {
    case "artery" => ApplicationModes.Artery
    case "netty" => ApplicationModes.Netty
    case _ => throw new Exception("Invalid application mode. Only [artery/netty] allowed")
  }

  lazy val config: Config = {
    (mode match {
      case ApplicationModes.Artery =>
        ConfigFactory.parseString(
          s"""
            | artery {
            |   enabled: true
            |
            |   canonical.hostname = ${System.getenv("HOSTNAME")}
            |   canonical.port = ${System.getenv("PORT")}
            |
            |   bind.hostname = ${System.getenv("BIND_HOSTNAME")}
            |   bind.port = ${System.getenv("BIND_HOSTNAME")}
            |
            |   advanced.outbound-message-queue-size = 4096
            |   advanced.flight-recorder.enabled = true
            |   advanced.idle-cpu-level = 10
            |
            |   large-message-destinations = [
            |     "/user/subscriberLarge"
            |   ]
            | }
          """.stripMargin)

      case ApplicationModes.Netty =>
        ConfigFactory.parseString(
          s"""
            | akka.remote.enabled-transports = ["akka.remote.netty.tcp"]
            | netty.tcp {
            |   hostname = ${System.getenv("HOSTNAME")}
            |   port = ${System.getenv("PORT")}
            |
            |   bind-hostname = ${System.getenv("BIND_HOSTNAME")}
            |   bind-port = ${System.getenv("BIND_PORT")}
            | }
          """.stripMargin)
    }).withFallback(ConfigFactory.load())
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("default")
    implicit val dispatcher = system.dispatcher

    role match {
      case ApplicationRoles.Publisher =>
        val smallMessagesSize = config.getInt("app.publisher.small-messages-size")
        val smallMessagesCount = config.getInt("app.publisher.small-messages-count")

        val largeMessagesSize = config.getInt("app.publisher.large-messages-size")
        val largeMessagesCount = config.getInt("app.publisher.large-messages-count")

        Future.sequence(Seq(
          resolveSubscriber("user/subscriberSmall"),
          resolveSubscriber("user/subscriberLarge")
        )).map {
          case Seq(small, large) =>
            println(s"Subscribers resolved in [$small, $large]")
            system.actorOf(Publisher.props(small, 100.millis, smallMessagesSize, smallMessagesCount))
            system.actorOf(Publisher.props(large, 500.millis, largeMessagesSize, largeMessagesCount))
        }.recover {
          case th =>
            println("Cannot resolve subscribers")
            println(th)
            system.terminate()
        }

      case ApplicationRoles.Subscriber =>
        system.actorOf(Subscriber.props(), "subscriberSmall")
        system.actorOf(Subscriber.props(), "subscriberLarge")
    }

    println(s"Running $role with $mode")
  }

  private def resolveSubscriber(path: String)(implicit system: ActorSystem): Future[ActorRef] = {
    val prefix = mode match {
      case ApplicationModes.Artery => "akka://default@subscriber:25520/"
      case ApplicationModes.Netty => "akka.tcp://default@subscriber:2552/"
    }

    system.actorSelection(s"$prefix$path").resolveOne(10.seconds)
  }
}
