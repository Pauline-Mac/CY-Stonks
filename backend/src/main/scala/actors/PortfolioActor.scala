package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object PortfolioActor {

  sealed trait Command
  case class AddAsset(name: String, quantity: Int) extends Command
  case class GetPortfolio(replyTo: ActorRef[Portfolio]) extends Command

  case class Portfolio(assets: Map[String, Int])

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    var portfolio = Portfolio(Map.empty)

    Behaviors.receiveMessage {
      case AddAsset(name, quantity) =>
        portfolio = Portfolio(portfolio.assets + (name -> (portfolio.assets.getOrElse(name, 0) + quantity)))
        context.log.info(s"Ajouté : $quantity de $name")
        Behaviors.same

      case GetPortfolio(replyTo) =>
        replyTo ! portfolio
        Behaviors.same
    }
  }
}
