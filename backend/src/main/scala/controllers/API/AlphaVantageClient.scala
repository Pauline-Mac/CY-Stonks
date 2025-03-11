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
import scala.util.{Failure, Success, Try}
import akka.actor.typed.scaladsl.adapter._
import akka.stream.Materializer
import akka.http.scaladsl.unmarshalling.Unmarshaller

object AlphaVantageClient {
  sealed trait Command

  case class GetStockPrice(symbol: String, replyTo: ActorRef[Response]) extends Command
  case class GetTopGainers(replyTo: ActorRef[Response]) extends Command
  case class GetRSI(symbol: String, replyTo: ActorRef[Response]) extends Command
  private case class HandleHttpResponse(response: Try[HttpResponse], replyTo: ActorRef[Response], messageType: String, symbol: Option[String] = None) extends Command
  private case class HandleEntityData(data: Try[String], replyTo: ActorRef[Response], messageType: String, symbol: Option[String] = None) extends Command

  sealed trait Response
  case class StockPriceResponse(symbol: String, price: Option[Double]) extends Response
  case class TopGainersResponse(gainers: List[String]) extends Response
  case class RSIResponse(symbol: String, rsi: Option[Double]) extends Response
  case class ErrorResponse(message: String) extends Response

  def apply()(implicit typedSystem: ActorSystem[_]): Behavior[Command] = {
    Behaviors.setup { context =>
      implicit val ec: ExecutionContext = context.system.executionContext
      implicit val classicSystem: akka.actor.ActorSystem = typedSystem.toClassic
      implicit val materializer: Materializer = Materializer.createMaterializer(classicSystem)

      val apiKey: String = sys.props.getOrElse("ALPHA_VANTAGE_API_KEY",
        sys.env.getOrElse("ALPHA_VANTAGE_API_KEY", {
          context.log.error("API key not found in properties or environment variables!")
          throw new RuntimeException("API key not defined")
        })
      )

      context.log.info("AlphaVantageClient actor initialized with API key")

      Behaviors.receiveMessage {
        case GetStockPrice(symbol, replyTo) =>
          context.log.info(s"Processing GetStockPrice request for $symbol")

          val url = s"https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=$symbol&interval=5min&apikey=$apiKey"
          context.log.debug(s"Requesting data from: $url")

          val request = HttpRequest(uri = url)
          val responseFuture = Http().singleRequest(request)

          context.pipeToSelf(responseFuture) {
            result => HandleHttpResponse(result, replyTo, "StockPrice", Some(symbol))
          }

          Behaviors.same

        case GetTopGainers(replyTo) =>
          context.log.info("Processing GetTopGainers request")

          val url = s"https://www.alphavantage.co/query?function=TOP_GAINERS_LOSERS&apikey=$apiKey"
          context.log.debug(s"Requesting data from: $url")

          val request = HttpRequest(uri = url)
          val responseFuture = Http().singleRequest(request)

          context.pipeToSelf(responseFuture) {
            result => HandleHttpResponse(result, replyTo, "TopGainers")
          }

          Behaviors.same

        case GetRSI(symbol, replyTo) =>
          context.log.info(s"Processing GetRSI request for $symbol")

          val url = s"https://www.alphavantage.co/query?function=RSI&symbol=$symbol&interval=5min&time_period=14&series_type=close&apikey=$apiKey"
          context.log.debug(s"Requesting data from: $url")

          val request = HttpRequest(uri = url)
          val responseFuture = Http().singleRequest(request)

          context.pipeToSelf(responseFuture) {
            result => HandleHttpResponse(result, replyTo, "RSI", Some(symbol))
          }

          Behaviors.same

        case HandleHttpResponse(Success(httpResponse), replyTo, messageType, symbolOpt) =>
          context.log.debug(s"Received HTTP response for $messageType with status: ${httpResponse.status}")

          if (httpResponse.status.isSuccess()) {
            val entityFuture: Future[String] = Unmarshal(httpResponse.entity).to[String]

            context.pipeToSelf(entityFuture) {
              result => HandleEntityData(result, replyTo, messageType, symbolOpt)
            }
          } else {
            httpResponse.entity.discardBytes()
            context.log.error(s"API request failed with status: ${httpResponse.status}")
            replyTo ! ErrorResponse(s"API request failed with status: ${httpResponse.status}")
          }

          Behaviors.same

        case HandleHttpResponse(Failure(exception), replyTo, messageType, _) =>
          context.log.error(s"HTTP request for $messageType failed: ${exception.getMessage}")
          replyTo ! ErrorResponse(s"Failed to connect to API: ${exception.getMessage}")
          Behaviors.same

        case HandleEntityData(Success(data), replyTo, messageType, symbolOpt) =>
          try {
            implicit val formats: Formats = DefaultFormats
            val json = parse(data)

            val response: Response = messageType match {
              case "StockPrice" =>
                val symbol = symbolOpt.getOrElse("Unknown")
                val timeSeriesData = (json \ "Time Series (5min)")
                if (timeSeriesData != JNothing) {
                  val price = timeSeriesData.children.headOption.flatMap { item =>
                    (item \ "4. close").extractOpt[String].map(_.toDouble)
                  }
                  StockPriceResponse(symbol, price)
                } else {
                  context.log.warn(s"No time series data found in response for $symbol")
                  StockPriceResponse(symbol, None)
                }

              case "TopGainers" =>
                val gainersData = (json \ "top_gainers")
                if (gainersData != JNothing) {
                  val gainers = gainersData.children.flatMap { item =>
                    (item \ "ticker").extractOpt[String]
                  }.toList
                  TopGainersResponse(gainers)
                } else {
                  context.log.warn("No gainers data found in response")
                  TopGainersResponse(List.empty)
                }

              case "RSI" =>
                val symbol = symbolOpt.getOrElse("Unknown")
                val rsiData = (json \ "Technical Analysis: RSI")
                if (rsiData != JNothing) {
                  val rsi = rsiData.children.headOption.flatMap { item =>
                    (item \ "RSI").extractOpt[String].map(_.toDouble)
                  }
                  RSIResponse(symbol, rsi)
                } else {
                  context.log.warn(s"No RSI data found in response for $symbol")
                  RSIResponse(symbol, None)
                }

              case _ =>
                context.log.error(s"Unknown message type: $messageType")
                ErrorResponse(s"Unknown message type: $messageType")
            }

            replyTo ! response

          } catch {
            case e: Exception =>
              context.log.error(s"Error parsing JSON: ${e.getMessage}")
              replyTo ! ErrorResponse(s"Failed to parse API response: ${e.getMessage}")
          }

          Behaviors.same

        case HandleEntityData(Failure(exception), replyTo, _, _) =>
          context.log.error(s"Failed to unmarshal response: ${exception.getMessage}")
          replyTo ! ErrorResponse(s"Failed to read API response: ${exception.getMessage}")
          Behaviors.same
      }
    }
  }
}
