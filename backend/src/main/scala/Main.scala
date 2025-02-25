import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import org.mindrot.jbcrypt.BCrypt
import actors.UserActor

object Main extends App {
  val system = ActorSystem(Behaviors.setup[UserActor.Portfolio] { context =>
    // Create a user actor
    val userActor = context.spawn(UserActor(), "user-1")

    // Add assets to the user portfolio
    userActor ! UserActor.AddAsset("AAPL", 10)
    userActor ! UserActor.AddAsset("BTC", 2)

    // Fetch and print the user's portfolio
    userActor ! UserActor.GetPortfolio(context.self)

    // Insert the user into the database with a hashed password using BCrypt
    val password = "securePassword123"
    val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt()) // Hash the password

    val userCreatedActor = context.spawn(Behaviors.receiveMessage[Boolean] {
      case true =>
        println("User successfully created in the database!")
        Behaviors.same
      case false =>
        println("Failed to create user in the database.")
        Behaviors.same
    }, "userCreatedActor")


    // Create a new user and insert into the database with the hashed password
    userActor ! UserActor.CreateUser("Pauline Maceiras", "pauline.maceiras@example.com", hashedPassword, userCreatedActor)

    // Receive the portfolio and print it out
    Behaviors.receiveMessage {
      case portfolio @ UserActor.Portfolio(assets) =>
        println(s"User portfolio: $assets")
        Behaviors.stopped
    }
  }, "PortfolioSystem")

  // Give it time to process
  Thread.sleep(2000)
}
