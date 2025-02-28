package controllers.API

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import actors.UserActor
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import controllers._  // Import all the response classes

class ApiServer(alphaVantageClient: ActorRef[AlphaVantageClient.Command],
                userActor: ActorRef[UserActor.Command])
               (implicit system: ActorSystem[_]) extends JsonFormats {

  implicit val executionContext: ExecutionContext = system.executionContext
  implicit val timeout: Timeout = 5.seconds

  // CORS directive
  private def corsHandler(route: Route) = {
    respondWithHeaders(
      headers.`Access-Control-Allow-Origin`.*,
      headers.`Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE, HttpMethods.OPTIONS),
      headers.`Access-Control-Allow-Headers`("Content-Type", "X-Requested-With")
    ) {
      route ~ options {
        complete(StatusCodes.OK)
      }
    }
  }


  // Health check route
  val healthRoute: Route = path("health") {
    get {
      complete(HealthResponse("healthy", "Backend is running"))
    }
  }

  // Sample data route
  val dataRoute: Route = path("api" / "data") {
    get {
      // Use a manually constructed JSON string to avoid format issues
      val jsonString = """[
        {"id": 1, "symbol": "AAPL", "price": 150.25, "change": 2.5},
        {"id": 2, "symbol": "MSFT", "price": 250.75, "change": -1.2},
        {"id": 3, "symbol": "GOOGL", "price": 2800.10, "change": 0.5}
      ]"""
      complete(HttpEntity(ContentTypes.`application/json`, jsonString))
    }
  }

  // Portfolio route
  val portfolioRoute: Route = path("api" / "portfolio") {
    get {
      val portfolioFuture: Future[UserActor.Portfolio] =
        userActor.ask(ref => UserActor.GetPortfolio(ref))

      onComplete(portfolioFuture) {
        case scala.util.Success(UserActor.Portfolio(assets)) =>
          val assetList = assets.map { case (symbol, quantity) =>
            Asset(symbol, quantity)
          }.toList
          complete(PortfolioResponse(assetList))

        case scala.util.Failure(ex) =>
          complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }

  // Stock price route
  val stockPriceRoute: Route = path("api" / "stock" / Segment) { symbol =>
    get {
      val priceFuture: Future[AlphaVantageClient.Response] =
        alphaVantageClient.ask(ref => AlphaVantageClient.GetStockPrice(symbol, ref))

      onComplete(priceFuture) {
        case scala.util.Success(response) =>
          response match {
            case AlphaVantageClient.StockPriceResponse(sym, price) =>
              complete(controllers.StockPriceResponse(sym, price))
            case AlphaVantageClient.ErrorResponse(message) =>
              complete(StatusCodes.InternalServerError, message)
            case _ =>
              complete(StatusCodes.InternalServerError, "Unexpected response type")
          }

        case scala.util.Failure(ex) =>
          complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }

  // RSI route
  val rsiRoute: Route = path("api" / "rsi" / Segment) { symbol =>
    get {
      val rsiFuture: Future[AlphaVantageClient.Response] =
        alphaVantageClient.ask(ref => AlphaVantageClient.GetRSI(symbol, ref))

      onComplete(rsiFuture) {
        case scala.util.Success(response) =>
          response match {
            case AlphaVantageClient.RSIResponse(sym, value) =>
              complete(controllers.RSIResponse(sym, value))
            case AlphaVantageClient.ErrorResponse(message) =>
              complete(StatusCodes.InternalServerError, message)
            case _ =>
              complete(StatusCodes.InternalServerError, "Unexpected response type")
          }

        case scala.util.Failure(ex) =>
          complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }

  // Combine all routes
  val routes: Route = corsHandler {
    healthRoute ~ dataRoute ~ portfolioRoute ~ stockPriceRoute ~ rsiRoute
  }

  // Start the HTTP server
  def start(host: String = "0.0.0.0", port: Int = 9000): Future[Http.ServerBinding] = {
    Http().newServerAt(host, port).bind(routes)
      .map { binding =>
        println(s"Server online at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/")
        binding
      }
  }
}
