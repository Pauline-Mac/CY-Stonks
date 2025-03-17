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
  final case class GetPortfolio(portfolioId: Int, replyTo: ActorRef[GetPortfolioResponse]) extends Command
  final case class DeletePortfolio(portfolioId: Int, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetPortfolioResponse(maybePortfolio: Option[Portfolio])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(portfolios: Set[Portfolio]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetPortfolios(replyTo) =>
        replyTo ! Portfolios(portfolios.toSeq)
        Behaviors.same

      case CreatePortfolio(portfolio, replyTo) =>
        replyTo ! ActionPerformed(s"Portfolio ${portfolio.portfolioId} created.")
        registry(portfolios + portfolio)

      case GetPortfolio(portfolioId, replyTo) =>
        replyTo ! GetPortfolioResponse(portfolios.find(_.portfolioId == portfolioId))
        Behaviors.same

      case DeletePortfolio(portfolioId, replyTo) =>
        replyTo ! ActionPerformed(s"Portfolio $portfolioId deleted.")
        registry(portfolios.filterNot(_.portfolioId == portfolioId))
    }
}
