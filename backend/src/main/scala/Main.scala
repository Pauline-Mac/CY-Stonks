import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import actors.UserActor

object Main extends App {
  val system = ActorSystem(Behaviors.setup[UserActor.Portfolio] { context =>
    val userActor = context.spawn(UserActor(), "user-1")

    // Add assets to the user portfolio
    userActor ! UserActor.AddAsset("AAPL", 10)
    userActor ! UserActor.AddAsset("BTC", 2)

    // Fetch the user's portfolio
    userActor ! UserActor.GetPortfolio(context.self)

    // Insert a new user into the database
    try {
      DataBaseControler.insertUser("Pauline Maceiras", "pauline.maceiras@example.com")
      println("____________________________We just added a new user__________________________________")
    } catch {
      case e: Exception => println(s"Error adding user: ${e.getMessage}")
    }

    // Receive and process the portfolio
    Behaviors.receiveMessage {
      case portfolio @ UserActor.Portfolio(assets) =>
        println(s"User portfolio: $assets")
        Behaviors.stopped
    }
  }, "PortfolioSystem")
  Thread.sleep(2000) // Give it 2 seconds to finish processing
}
