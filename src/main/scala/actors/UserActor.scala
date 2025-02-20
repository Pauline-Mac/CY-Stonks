package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object UserActor {
  sealed trait Command

  case class AddAsset(name: String, quantity: Int) extends Command

  case class GetPortfolio(replyTo: ActorRef[Portfolio]) extends Command

  case class Portfolio(assets: Map[String, Int])

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    var portfolio = Portfolio(Map.empty)

    Behaviors.receiveMessage {
      case AddAsset(name, quantity) =>
        portfolio = Portfolio(portfolio.assets + (name -> (portfolio.assets.getOrElse(name, 0) + quantity)))
        println(s"Added: $quantity of $name")
        Behaviors.same

      case GetPortfolio(replyTo) =>
        replyTo ! portfolio
        Behaviors.same
    }
  }
}

object Main extends App {
  import actors.UserActor
  import akka.actor.typed.ActorSystem
  import akka.actor.typed.scaladsl.Behaviors

  val system = ActorSystem(Behaviors.setup[UserActor.Portfolio] { context =>
    // Create a user actor
    val userActor = context.spawn(UserActor(), "user-1")

    // Add assets to the user portfolio
    userActor ! UserActor.AddAsset("AAPL", 10)
    userActor ! UserActor.AddAsset("BTC", 2)

    // Fetch and print the user's portfolio
    userActor ! UserActor.GetPortfolio(context.self)

    // Receive the portfolio and print it out
    Behaviors.receiveMessage {
      case UserActor.Portfolio(assets) =>
        println(s"User portfolio: $assets")
        Behaviors.stopped
    }
  }, "PortfolioSystem")
}
