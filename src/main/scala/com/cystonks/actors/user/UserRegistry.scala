package com.cystonks.actors.user

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable
import com.cystonks.models.{User, Users}

object UserRegistry {
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUser(uuid: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(uuid: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetUserResponse(maybeUser: Option[User])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(users: Set[User]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetUsers(replyTo) =>
        replyTo ! Users(users.toSeq)
        Behaviors.same
      
      case CreateUser(user, replyTo) =>
        replyTo ! ActionPerformed(s"User ${user.uuid} created.")
        registry(users + user)
      
      case GetUser(uuid, replyTo) =>
        replyTo ! GetUserResponse(users.find(_.uuid == uuid))
        Behaviors.same
      
      case DeleteUser(uuid, replyTo) =>
        replyTo ! ActionPerformed(s"User $uuid deleted.")
        registry(users.filterNot(_.uuid == uuid))
    }
}
