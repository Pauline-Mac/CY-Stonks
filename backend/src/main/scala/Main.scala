import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.http.scaladsl.model.StatusCodes
import akka.util.ByteString
import scala.concurrent.{ExecutionContextExecutor, Future}
import org.mindrot.jbcrypt.BCrypt
import actors.UserActor
import controllers.API.{AlphaVantageClient, ApiServer}
import controllers.DataBaseController
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

object Main extends App{
  def printCyStonksMessage(): Unit = {
    println("Welcome to CyStonks!")
    println(""" ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀ """)
  }


  // Create actor system
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "CyStonksSystem")
  implicit val executionContext: ExecutionContext = system.executionContext

  // Print welcome message
  printCyStonksMessage()

  // Create a user actor
  val userActor = system.systemActorOf(UserActor(), "userActor")

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

  // Create the AlphaVantage client actor
  val alphaVantageClient = system.systemActorOf(AlphaVantageClient(), "alphaVantageClient")

  // Create a response handler actor
  val responseHandler = system.systemActorOf(
    Behaviors.receiveMessage[AlphaVantageClient.Response] {
      case AlphaVantageClient.StockPriceResponse(symbol, Some(price)) =>
        println(s"✅ Received stock price for $symbol: $price")
        Behaviors.same
      case AlphaVantageClient.StockPriceResponse(symbol, None) =>
        println(s"❌ No stock price data available for $symbol")
        Behaviors.same
      case AlphaVantageClient.TopGainersResponse(gainers) =>
        println("✅ Received top gainers:")
        gainers.foreach(ticker => println(s"- $ticker"))
        Behaviors.same
      case AlphaVantageClient.RSIResponse(symbol, Some(rsi)) =>
        println(s"✅ Received RSI for $symbol: $rsi")
        Behaviors.same
      case AlphaVantageClient.RSIResponse(symbol, None) =>
        println(s"❌ No RSI data available for $symbol")
        Behaviors.same
      case AlphaVantageClient.ErrorResponse(message) =>
        println(s"🚨 Error: $message")
        Behaviors.same
    },
    "responseHandler"
  )

  // Create and start the API server
  val apiServer = new ApiServer(alphaVantageClient, userActor)
  val serverBinding = apiServer.start()



  // Send test requests
  println("Sending request for AAPL stock price...")
  alphaVantageClient ! AlphaVantageClient.GetStockPrice("AAPL", responseHandler)

  Thread.sleep(3000) // Wait a bit between requests to avoid API rate limits

  println("Sending request for top gainers...")
  alphaVantageClient ! AlphaVantageClient.GetTopGainers(responseHandler)

  Thread.sleep(3000)

  println("Sending request for MSFT RSI...")
  alphaVantageClient ! AlphaVantageClient.GetRSI("MSFT", responseHandler)

  // Keep the system alive
  println("System running, press Ctrl+C to terminate...")
  Await.result(system.whenTerminated, Duration.Inf)
}

