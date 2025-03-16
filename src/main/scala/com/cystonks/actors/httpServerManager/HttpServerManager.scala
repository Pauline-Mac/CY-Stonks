package com.cystonks.actors.httpservermanager

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.cystonks.actors.httpserver.HttpServer

object HttpServerManager {
  sealed trait ServerCommand
  final case class GetSession(screenName: String, replyTo: ActorRef[SessionEvent]) extends ServerCommand
  private final case class PublishSessionMessage(screenName: String, message: String) extends ServerCommand

  sealed trait SessionEvent
  final case class SessionGranted(handle: ActorRef[PostMessage]) extends SessionEvent
  final case class SessionDenied(reason: String) extends SessionEvent
  final case class MessagePosted(screenName: String, message: String) extends SessionEvent

  sealed trait SessionCommand
  final case class PostMessage(message: String) extends SessionCommand
  private final case class NotifyClient(message: MessagePosted) extends SessionCommand

  def apply(): Behavior[ServerCommand] =
    httpServerManager(List.empty)

  private def httpServerManager(sessions: List[ActorRef[SessionCommand]]): Behavior[ServerCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetSession(screenName, client) =>
          val ses = context.spawn(
            session(context.self, screenName, client),
            name = URLEncoder.encode(screenName, StandardCharsets.UTF_8.name))
          client ! SessionGranted(ses)
          httpServerManager(ses :: sessions)
        case PublishSessionMessage(screenName, message) =>
          val notification = NotifyClient(MessagePosted(screenName, message))
          sessions.foreach(_ ! notification)
          Behaviors.same
      }
    }

  private def session(
      room: ActorRef[PublishSessionMessage],
      screenName: String,
      client: ActorRef[SessionEvent]): Behavior[SessionCommand] =
    Behaviors.receiveMessage {
      case PostMessage(message) =>
        room ! PublishSessionMessage(screenName, message)
        Behaviors.same
      case NotifyClient(message) =>
        client ! message
        Behaviors.same
    }
}