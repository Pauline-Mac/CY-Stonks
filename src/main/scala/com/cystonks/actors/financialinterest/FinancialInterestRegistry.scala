package com.cystonks.actors.financialinterest

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable
import com.cystonks.models.{FinancialInterest, FinancialInterests}

object FinancialInterestRegistry {
  sealed trait Command

  final case class GetFinancialInterests(replyTo: ActorRef[FinancialInterests]) extends Command
  final case class CreateFinancialInterest(financialInterest: FinancialInterest, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetFinancialInterest(uuid: String, replyTo: ActorRef[GetFinancialInterestResponse]) extends Command
  final case class DeleteFinancialInterest(uuid: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetFinancialInterestResponse(maybeFinancialInterest: Option[FinancialInterest])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(financialInterests: Set[FinancialInterest]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetFinancialInterests(replyTo) =>
        replyTo ! FinancialInterests(financialInterests.toSeq)
        Behaviors.same
      
      case CreateFinancialInterest(financialInterest, replyTo) =>
        replyTo ! ActionPerformed(s"Financial Interest ${financialInterest.uuid} created.")
        registry(financialInterests + financialInterest)
      
      case GetFinancialInterest(uuid, replyTo) =>
        replyTo ! GetFinancialInterestResponse(financialInterests.find(_.uuid == uuid))
        Behaviors.same
      
      case DeleteFinancialInterest(uuid, replyTo) =>
        replyTo ! ActionPerformed(s"Financial Interest $uuid deleted.")
        registry(financialInterests.filterNot(_.uuid == uuid))
    }
}
