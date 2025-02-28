package controllers.API

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.json4s._
import org.json4s.native.JsonMethods._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AlphaVantageClient {
  // Define the message protocol for the actor
  sealed trait Command

  // Commands (requests)
  case class GetStockPrice(symbol: String, replyTo: ActorRef[Response]) extends Command
  case class GetTopGainers(replyTo: ActorRef[Response]) extends Command
  case class GetRSI(symbol: String, replyTo: ActorRef[Response]) extends Command

  // Responses
  sealed trait Response
  case class StockPriceResponse(symbol: String, price: Option[Double]) extends Response
  case class TopGainersResponse(gainers: List[String]) extends Response
  case class RSIResponse(symbol: String, rsi: Option[Double]) extends Response
  case class ErrorResponse(message: String) extends Response

  def apply()(implicit system: ActorSystem[_]): Behavior[Command] = {
    Behaviors.setup { context =>
      implicit val ec: ExecutionContext = context.system.executionContext

      // Get API key from environment variables
      val apiKey: String = sys.env.getOrElse("ALPHA_VANTAGE_API_KEY",
        throw new RuntimeException("API key not defined in environment variables"))

      Behaviors.receiveMessage {
        case GetStockPrice(symbol, replyTo) =>
          context.log.info(s"Fetching stock price for $symbol")
          val url = s"https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=$symbol&interval=5min&apikey=$apiKey"

          val responseFuture = Http().singleRequest(HttpRequest(uri = url))
            .flatMap(response => Unmarshal(response.entity).to[String])
            .map { data =>
              implicit val formats: Formats = DefaultFormats
              val json = parse(data)
              val price = (json \ "Time Series (5min)").children.headOption.flatMap { item =>
                (item \ "4. close").extractOpt[Double]
              }
              StockPriceResponse(symbol, price)
            }

          responseFuture.onComplete {
            case Success(response) => replyTo ! response
            case Failure(exception) =>
              context.log.error(s"Error fetching stock price: ${exception.getMessage}")
              replyTo ! ErrorResponse(exception.getMessage)
          }

          Behaviors.same

        case GetTopGainers(replyTo) =>
          context.log.info("Fetching top gainers")
          val url = s"https://www.alphavantage.co/query?function=TOP_GAINERS_LOSERS&apikey=$apiKey"

          val responseFuture = Http().singleRequest(HttpRequest(uri = url))
            .flatMap(response => Unmarshal(response.entity).to[String])
            .map { data =>
              implicit val formats: Formats = DefaultFormats
              val json = parse(data)
              val gainers = (json \ "top_gainers").children.flatMap { item =>
                (item \ "ticker").extractOpt[String]
              }.toList // Ensure it's a List
              TopGainersResponse(gainers)
            }

          responseFuture.onComplete {
            case Success(response) => replyTo ! response
            case Failure(exception) =>
              context.log.error(s"Error fetching top gainers: ${exception.getMessage}")
              replyTo ! ErrorResponse(exception.getMessage)
          }

          Behaviors.same

        case GetRSI(symbol, replyTo) =>
          context.log.info(s"Fetching RSI for $symbol")
          val url = s"https://www.alphavantage.co/query?function=RSI&symbol=$symbol&interval=5min&time_period=14&series_type=close&apikey=$apiKey"

          val responseFuture = Http().singleRequest(HttpRequest(uri = url))
            .flatMap(response => Unmarshal(response.entity).to[String])
            .map { data =>
              implicit val formats: Formats = DefaultFormats
              val json = parse(data)
              val rsi = (json \ "Technical Analysis: RSI").children.headOption.flatMap { item =>
                (item \ "RSI").extractOpt[Double]
              }
              RSIResponse(symbol, rsi)
            }

          responseFuture.onComplete {
            case Success(response) => replyTo ! response
            case Failure(exception) =>
              context.log.error(s"Error fetching RSI: ${exception.getMessage}")
              replyTo ! ErrorResponse(exception.getMessage)
          }

          Behaviors.same
      }
    }
  }
}
