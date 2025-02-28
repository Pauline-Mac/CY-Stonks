import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.ActorSystem => ClassicActorSystem
import org.mindrot.jbcrypt.BCrypt
import actors.UserActor
import controllers.API.AlphaVantageClient

object Main extends App {
  // Function to print the CyStonks message
  def printCyStonksMessage(): Unit = {
    println("Welcome to CyStonks!")
    println(""" ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀ """)
  }

  // Create the typed actor system for portfolio management
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "PortfolioSystem")

  // Create the AlphaVantage client actor
  val alphaVantageClient = system.systemActorOf(AlphaVantageClient(), "alphaVantageClient")

  // Create a response handler actor
  val responseHandler = system.systemActorOf(
    Behaviors.receiveMessage[AlphaVantageClient.Response] {
      case AlphaVantageClient.StockPriceResponse(symbol, Some(price)) =>
        println(s"Current price of $symbol: $price")
        Behaviors.same
      case AlphaVantageClient.StockPriceResponse(symbol, None) =>
        println(s"No price data available for $symbol")
        Behaviors.same
      case AlphaVantageClient.TopGainersResponse(gainers) =>
        println("Top Gainers:")
        gainers.foreach(ticker => println(s"- $ticker"))
        Behaviors.same
      case AlphaVantageClient.RSIResponse(symbol, Some(rsi)) =>
        println(s"Current RSI of $symbol: $rsi")
        Behaviors.same
      case AlphaVantageClient.RSIResponse(symbol, None) =>
        println(s"No RSI data available for $symbol")
        Behaviors.same
      case AlphaVantageClient.ErrorResponse(message) =>
        println(s"Error: $message")
        Behaviors.same
    },
    "responseHandler"
  )

  // Make multiple API calls
  alphaVantageClient ! AlphaVantageClient.GetStockPrice("AAPL", responseHandler)
  alphaVantageClient ! AlphaVantageClient.GetTopGainers(responseHandler)
  alphaVantageClient ! AlphaVantageClient.GetRSI("MSFT", responseHandler)

  // Functionality for portfolio management and user management
  system.systemActorOf(
    Behaviors.setup[UserActor.Portfolio] { context =>
      printCyStonksMessage()

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
          Behaviors.same
      }
    },
    "portfolioReceiver"
  )

  // Wait to receive the stock data (add sleep to give the request time to complete)
  Thread.sleep(5000)
}
