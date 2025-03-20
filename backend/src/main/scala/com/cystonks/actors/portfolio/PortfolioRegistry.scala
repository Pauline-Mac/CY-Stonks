package com.cystonks.actors.portfolio

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import slick.jdbc.PostgresProfile.api._
import com.cystonks.config.DatabaseConfig._
import com.cystonks.models.{Portfolio, Portfolios}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object PortfolioRegistry {
  sealed trait Command
  final case class GetPortfolios(replyTo: ActorRef[Portfolios]) extends Command
  final case class CreatePortfolio(portfolio: Portfolio, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetPortfolio(portfolioId: Int, replyTo: ActorRef[GetPortfolioResponse]) extends Command
  final case class DeletePortfolio(portfolioId: Int, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetPortfolioResponse(maybePortfolio: Option[Portfolio])
  final case class ActionPerformed(description: String)

  def apply()(implicit ec: ExecutionContext): Behavior[Command] = registry(Set.empty)

  private def registry(portfolios: Set[Portfolio])(implicit ec: ExecutionContext): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val ec: ExecutionContext = context.executionContext

      Behaviors.receiveMessage {
        case GetPortfolios(replyTo) =>
          replyTo ! Portfolios(portfolios.toSeq)
          Behaviors.same

        case CreatePortfolio(portfolio, replyTo) =>
          val insertPortfolioAction =
            sqlu"""
              INSERT INTO portfolios (portfolio_id, user_id, portfolio_name)
              VALUES (${portfolio.portfolioId}, uuid(${portfolio.userUuid}), ${portfolio.name})
              ON CONFLICT (portfolio_id) DO NOTHING
            """

          val insertFuture: Future[Int] = db.run(insertPortfolioAction)

          insertFuture.onComplete {
            case Success(_) =>
              replyTo ! ActionPerformed(s"Portfolio ${portfolio.portfolioId} created.")
              context.self ! UpdateRegistry(Set(portfolio))
            case Failure(ex) =>
              replyTo ! ActionPerformed(s"Failed to create portfolio ${portfolio.portfolioId}: ${ex.getMessage}")
          }

          Behaviors.same

        case UpdateRegistry(updatedPortfolios) =>
          registry(updatedPortfolios) // Return a new `Behavior` with the updated registry

        case GetPortfolio(portfolioId, replyTo) =>
          replyTo ! GetPortfolioResponse(portfolios.find(_.portfolioId == portfolioId))
          Behaviors.same

        case DeletePortfolio(portfolioId, replyTo) =>
          val deletePortfolioAction =
            sqlu"""
            DELETE FROM portfolios WHERE portfolio_id = $portfolioId
          """
          val deleteFuture: Future[Int] = db.run(deletePortfolioAction)
          deleteFuture.onComplete {
            case Success(rowsAffected) =>
              if (rowsAffected > 0) {
                replyTo ! ActionPerformed(s"Portfolio $portfolioId deleted.")
                context.self ! UpdateRegistry(portfolios.filterNot(_.portfolioId == portfolioId))
              } else {
                replyTo ! ActionPerformed(s"Portfolio $portfolioId not found.")
              }
            case Failure(ex) =>
              replyTo ! ActionPerformed(s"Failed to delete portfolio $portfolioId: ${ex.getMessage}")
          }
          Behaviors.same
      }
    }

  private final case class UpdateRegistry(updatedPortfolios: Set[Portfolio]) extends Command
}
