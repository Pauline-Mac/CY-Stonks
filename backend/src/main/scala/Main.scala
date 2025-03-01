import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import org.mindrot.jbcrypt.BCrypt
import actors.UserActor
import controllers.API.{AlphaVantageClient, ApiServer}
import controllers.DataBaseController
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

object Main extends App {
  // Function to print the CyStonks message
  def printCyStonksMessage(): Unit = {
    println("Welcome to CyStonks!")
    println(""" ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀ """)
  }

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "CyStonksSystem")
  implicit val executionContext: ExecutionContext = system.executionContext

  // Print welcome message
  printCyStonksMessage()

  // Create the AlphaVantage client actor
  val alphaVantageClient = system.systemActorOf(AlphaVantageClient(), "alphaVantageClient")

  // Create a user actor
  val userActor = system.systemActorOf(UserActor(), "userActor")

  // Create a response handler actor for testing
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

  // Initialize user data
  // Hash a password using BCrypt
  val password = "securePassword123"
  val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

  // Create a user with hashed password
  val userCreatedActor = system.systemActorOf(
    Behaviors.receiveMessage[Boolean] {
      case true =>
        println("User is in the database!")

        // Add sample assets to the portfolio after user creation

        Behaviors.same
      case false =>
        println("Failed to create user in the database.")
        Behaviors.same
    },
    "userCreatedActor"
  )
  userActor ! UserActor.CreateUser("Pauline Maceiras", "pauline.maceiras@example.com", hashedPassword, userCreatedActor)

  // Create and start the API server
  val apiServer = new ApiServer(alphaVantageClient, userActor)
  val serverBinding = apiServer.start()

  serverBinding.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      println(s"Server online at http://${address.getHostString}:${address.getPort}/")

      // Make some test API calls
      alphaVantageClient ! AlphaVantageClient.GetStockPrice("AAPL", responseHandler)
      alphaVantageClient ! AlphaVantageClient.GetTopGainers(responseHandler)
      alphaVantageClient ! AlphaVantageClient.GetRSI("MSFT", responseHandler)

    case Failure(ex) =>
      println(s"Failed to bind HTTP endpoint, terminating system: ${ex.getMessage}")
      system.terminate()
  }

  // Keep the system alive
  Await.result(system.whenTerminated, Duration.Inf)

}
