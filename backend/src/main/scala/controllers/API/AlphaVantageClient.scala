package controllers.API

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.json4s._
import org.json4s.native.JsonMethods._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object AlphaVantageClient {
  implicit val system: akka.actor.typed.ActorSystem[Nothing] = akka.actor.typed.ActorSystem(Behaviors.empty, "AlphaVantageClient")
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  // Importing DefaultFormats to allow easy extraction of values from JSON
  implicit val formats: Formats = DefaultFormats

  val apiKey: String = sys.env.getOrElse("ALPHA_VANTAGE_API_KEY",
    throw new RuntimeException("Clé API non définie dans les variables d'environnement"))

  val url = s"https://www.alphavantage.co/query?function=TOP_GAINERS_LOSERS&apikey=$apiKey"

  // HTTP client to fetch stock data
  def getStockData(): Future[String] = {
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = url))
    responseFuture.flatMap { response =>
      Unmarshal(response.entity).to[String]
    }
  }

  // Extract top gainers' tickers from the JSON response
  def extractTopGainersTickers(data: String): List[String] = {
    val json = parse(data)
    (json \ "top_gainers").children.flatMap { item =>
      (item \ "ticker").extractOpt[String]
    }
  }

  // Actor to handle stock data retrieval
  def stockDataActor: Behavior[String] = Behaviors.receiveMessage { case _ =>
    val stockDataFuture = getStockData()

    stockDataFuture.onComplete {
      case Success(data) =>
        val tickers = extractTopGainersTickers(data)
        println("Top gainers tickers: " + tickers.mkString(", "))
      case Failure(exception) =>
        println("Erreur lors de la récupération des données : " + exception.getMessage)
    }(executionContext)

    Behaviors.same
  }
}
