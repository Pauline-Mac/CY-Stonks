package com.cystonks.actors.portfolio

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable
import com.cystonks.models.{Portfolio, Portfolios}

object PortfolioRegistry {
  sealed trait Command

  final case class GetPortfolios(replyTo: ActorRef[Portfolios]) extends Command
  final case class CreatePortfolio(portfolio: Portfolio, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetPortfolio(uuid: String, replyTo: ActorRef[GetPortfolioResponse]) extends Command
  final case class DeletePortfolio(uuid: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetPortfolioResponse(maybePortfolio: Option[Portfolio])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(portfolios: Set[Portfolio]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetPortfolios(replyTo) =>
        replyTo ! Portfolios(portfolios.toSeq)
        Behaviors.same
      
      case CreatePortfolio(portfolio, replyTo) =>
        replyTo ! ActionPerformed(s"Financial Interest ${portfolio.uuid} created.")
        registry(portfolios + portfolio)
      
      case GetPortfolio(uuid, replyTo) =>
        replyTo ! GetPortfolioResponse(portfolios.find(_.uuid == uuid))
        Behaviors.same
      
      case DeletePortfolio(uuid, replyTo) =>
        replyTo ! ActionPerformed(s"Financial Interest $uuid deleted.")
        registry(portfolios.filterNot(_.uuid == uuid))
    }
}
