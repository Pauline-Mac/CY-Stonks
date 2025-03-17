package com.cystonks

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration._

// Modèle de données pour les métadonnées
case class MetaData(
                     `1. Information`: String,
                     `2. Symbol`: String,
                     `3. Last Refreshed`: String,
                     `4. Output Size`: String,
                     `5. Time Zone`: String
                   )

// Modèle de données pour les prix quotidiens
case class DailyData(
                      `1. open`: String,
                      `2. high`: String,
                      `3. low`: String,
                      `4. close`: String,
                      `5. volume`: String
                    )

// Modèle de données pour la réponse complète
case class TimeSeriesDaily(
                            `Meta Data`: MetaData,
                            `Time Series (Daily)`: Map[String, DailyData]
                          )

// Formats JSON pour les modèles de données
object MyJsonProtocol extends DefaultJsonProtocol {
  implicit val dailyDataFormat: JsonFormat[DailyData] = jsonFormat5(DailyData)
  implicit val metaDataFormat: JsonFormat[MetaData] = jsonFormat5(MetaData)
  implicit val timeSeriesDailyFormat: JsonFormat[TimeSeriesDaily] = jsonFormat2(TimeSeriesDaily)
}


import MyJsonProtocol._


object Analyse {


  val apiKey = "7VFY9YKN5HL6787W"
  val symbol = "MSFT"
  val function = "TIME_SERIES_DAILY"
  val apiUrl = s"https://www.alphavantage.co/query?function=$function&symbol=$symbol&apikey=$apiKey"


  implicit val system: ActorSystem = ActorSystem("FinancialAnalysis")
  import system.dispatcher

  // Récupération des données depuis AlphaVantage
  def fetchData(): Future[TimeSeriesDaily] = {
    import spray.json._
    import MyJsonProtocol._

    val request = HttpRequest(uri = apiUrl)
    Http().singleRequest(request).flatMap { response =>
      response.entity.toStrict(5.seconds).map { strict =>
        strict.data.utf8String.parseJson.convertTo[TimeSeriesDaily]
      }
    }
  }
  e
  def main(args: Array[String]): Unit = {
    fetchData().onComplete {
      case Success(data) =>
        val closePrices = data.`Time Series (Daily)`.toList
          .sortBy(_._1)
          .map { case (_, dailyData) => dailyData.`4. close`.toDouble }

        // Calcul des rendements historiques
        val returns = calculateReturns(closePrices)

        // Calcul de la volatilité
        val volatility = calculateVolatility(returns)

        // Calcul du RSI
        val rsi = calculateRSI(returns)


        val (macd, signalLine, macdHistogram) = calculateMACD(closePrices)


        println(s"Rendements historiques : $returns")
        println(s"Volatilité (risque) : $volatility")
        println(s"RSI : $rsi")
        println(s"MACD : $macd, Signal Line : $signalLine, Histogramme : $macdHistogram")

        // Arrêt du système Akka
        system.terminate()

      case Failure(ex) =>
        println(s"Erreur lors de la récupération des données : ${ex.getMessage}")
        system.terminate()
    }
  }

  // Calcul des rendements historiques
  def calculateReturns(closePrices: List[Double]): List[Double] = {
    closePrices.sliding(2).map { case List(prevClose, currClose) =>
      (currClose - prevClose) / prevClose
    }.toList
  }

  // Calcul de la volatilité (risque)
  def calculateVolatility(returns: List[Double]): Double = {
    val meanReturn = returns.sum / returns.length
    val variance = returns.map(r => math.pow(r - meanReturn, 2)).sum / returns.length
    math.sqrt(variance)
  }

  // Calcul du RSI (Relative Strength Index)
  def calculateRSI(returns: List[Double], periods: Int = 14): Double = {
    val gains = returns.map(r => if (r > 0) r else 0)
    val losses = returns.map(r => if (r < 0) -r else 0)
    val avgGain = gains.takeRight(periods).sum / periods
    val avgLoss = losses.takeRight(periods).sum / periods
    val rs = avgGain / avgLoss
    100 - (100 / (1 + rs))
  }

  def calculateEMA(prices: List[Double], period: Int): Double = {
    val k = 2.0 / (period + 1.0)
    val initialEMA = prices.take(period).sum / period

    prices.drop(period).foldLeft(initialEMA) { (ema, price) =>
      price * k + ema * (1 - k)
    }
  }

  // Calcul du MACD (Moving Average Convergence Divergence)
  def calculateMACD(closePrices: List[Double]): (Double, Double, Double) = {
    val shortEMA = calculateEMA(closePrices, 12)
    val longEMA = calculateEMA(closePrices, 26)
    val macd = shortEMA - longEMA
    val signalLine = macd * 0.9
    val macdHistogram = macd - signalLine
    (macd, signalLine, macdHistogram)
  }
}