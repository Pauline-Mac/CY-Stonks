import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import actors.UserActor

object Main extends App {
  // Change the type parameter to match what GetPortfolio replies with
  val system = ActorSystem(Behaviors.setup[UserActor.Portfolio] { context =>
    val userActor = context.spawn(UserActor(), "user-1")

    // Add assets to the user portfolio
    userActor ! UserActor.AddAsset("AAPL", 10)
    userActor ! UserActor.AddAsset("BTC", 2)

    // Fetch the user's portfolio
    userActor ! UserActor.GetPortfolio(context.self)

    // Receive and process the portfolio
    Behaviors.receiveMessage {
      case portfolio @ UserActor.Portfolio(assets) =>
        println(s"User portfolio: $assets")
        Behaviors.stopped
    }
  }, "PortfolioSystem")
}