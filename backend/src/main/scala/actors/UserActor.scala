

package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import controllers.DataBaseController

object UserActor {
  sealed trait Command
  final case class AddAsset(name: String, quantity: Int) extends Command
  final case class GetPortfolio(replyTo: ActorRef[Portfolio]) extends Command
  final case class CreateUser(name: String, email: String, passwordHash: String, replyTo: ActorRef[Boolean]) extends Command

  final case class Portfolio(assets: Map[String, Int])

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

      case CreateUser(name, email, passwordHash, replyTo) =>
        try {
          DataBaseController.insertUser(name, email, passwordHash)
          replyTo ! true // Send success to replyTo actor
        } catch {
          case e: Exception =>
            println(s"Error adding user: ${e.getMessage}")
            replyTo ! false
        }
        Behaviors.same
    }
  }
}
