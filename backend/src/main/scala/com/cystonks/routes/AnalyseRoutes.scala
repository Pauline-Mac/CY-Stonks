package com.cystonks.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._
import spray.json.DefaultJsonProtocol
import spray.json.{DefaultJsonProtocol, JsObject, JsValue, RootJsonFormat, enrichAny, enrichString}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import scala.concurrent.duration._
import com.cystonks.config.AlphaVantageConfig


case class PriceData(
                      date: String,
                      price: Double
                    )

case class MetricInfo(
                       value: Double,
                       interpretation: String,
                       thresholds: Map[String, Double]
                     )

case class TechnicalAnalysis(
                              rsi: MetricInfo,
                              macd: MetricInfo,
                              volatility: MetricInfo
                            )

case class MarketAnalysis(
                           symbol: String,
                           assetType: String,
                           lastRefreshed: String,
                           currentPrice: Double,
                           priceChange: Double,
                           priceChangePercent: Double,
                           historicalPrices: List[PriceData],
                           technicalAnalysis: TechnicalAnalysis,
                           recommendation: String,
                           signals: Map[String, String]
                         )


object EnhancedJsonProtocol extends DefaultJsonProtocol {
  implicit val priceDataFormat: RootJsonFormat[PriceData] = jsonFormat2(PriceData)
  implicit val metricInfoFormat: RootJsonFormat[MetricInfo] = jsonFormat3(MetricInfo)
  implicit val technicalAnalysisFormat: RootJsonFormat[TechnicalAnalysis] = jsonFormat3(TechnicalAnalysis)
  implicit val marketAnalysisFormat: RootJsonFormat[MarketAnalysis] = jsonFormat10(MarketAnalysis)
}

class MarketAnalysisRoutes(implicit val system: ActorSystem[_]) {
  import EnhancedJsonProtocol._
  implicit val ec: ExecutionContext = system.executionContext

  val apiKey = AlphaVantageConfig.apiKey

  def fetchMarketData(symbol: String): Future[(JsObject, String, String)] = {

    val (function, apiParams, assetType) = if (isCryptoSymbol(symbol)) {
      ("DIGITAL_CURRENCY_DAILY", s"&symbol=$symbol&market=USD", "Cryptocurrency")
    } else {
      ("TIME_SERIES_DAILY", s"&symbol=IBM", "Stock")
    }

    val apiUrl = if (symbol.toLowerCase == "demo") {
      s"https://www.alphavantage.co/query?function=$function$apiParams&apikey=demo"
    } else {
      s"https://www.alphavantage.co/query?function=$function$apiParams&apikey=$apiKey"
    }

    val request = HttpRequest(uri = apiUrl)
    Http().singleRequest(request).flatMap { response =>
      if (response.status.isSuccess()) {
        response.entity.toStrict(5.seconds).map { strict =>
          import spray.json._
          val jsonData = strict.data.utf8String.parseJson.asJsObject
          (jsonData, function, assetType)
        }
      } else {
        Future.failed(new RuntimeException(s"API request failed with status ${response.status}"))
      }
    }
  }

  def extractPriceData(jsonData: JsObject, function: String): (String, List[(String, Double)]) = {
    import spray.json._

    function match {
      // Stock data extraction
      case "TIME_SERIES_DAILY" =>
        val metaData = jsonData.fields("Meta Data").asJsObject
        val lastRefreshed = metaData.fields("3. Last Refreshed").convertTo[String]
        val timeSeriesKey = "Time Series (Daily)"

        val priceMap = jsonData.fields(timeSeriesKey).asJsObject.fields.map { case (date, data) =>
          val dailyData = data.asJsObject
          val closePrice = dailyData.fields("4. close").convertTo[String].toDouble
          (date, closePrice)
        }.toList.sortBy(_._1)

        (lastRefreshed, priceMap)

      // Crypto data extraction
      case "DIGITAL_CURRENCY_DAILY" =>
        val metaData = jsonData.fields("Meta Data").asJsObject
        val lastRefreshed = metaData.fields("6. Last Refreshed").convertTo[String]
        val timeSeriesKey = "Time Series (Digital Currency Daily)"

        val priceMap = jsonData.fields(timeSeriesKey).asJsObject.fields.map { case (date, data) =>
          val dailyData = data.asJsObject
          val closePrice = dailyData.fields("4a. close (USD)").convertTo[String].toDouble
          (date, closePrice)
        }.toList.sortBy(_._1)

        (lastRefreshed, priceMap)

      // Default case for unsupported functions
      case _ =>
        throw new IllegalArgumentException(s"Unsupported API function: $function")
    }
  }

  // Helper to identify likely crypto symbols
  private def isCryptoSymbol(symbol: String): Boolean = {
    val commonCryptos = Set("BTC", "ETH", "XRP", "LTC", "BCH", "ADA", "DOT", "LINK", "XLM", "DOGE", "SOL", "AVAX", "MATIC")
    commonCryptos.contains(symbol.toUpperCase)
  }

  // Calculate returns from prices
  def calculateReturns(closePrices: List[Double]): List[Double] = {
    closePrices.sliding(2).map { case List(prevClose, currClose) =>
      (currClose - prevClose) / prevClose
    }.toList
  }

  // Calculate volatility
  def calculateVolatility(returns: List[Double]): Double = {
    if (returns.isEmpty) return 0.0
    val meanReturn = returns.sum / returns.length
    val variance = returns.map(r => math.pow(r - meanReturn, 2)).sum / returns.length
    math.sqrt(variance)
  }

  // Calculate RSI (Relative Strength Index)
  def calculateRSI(returns: List[Double], periods: Int = 14): Double = {
    if (returns.isEmpty) return 50.0 // Default value if not enough data

    val gains = returns.map(r => if (r > 0) r else 0)
    val losses = returns.map(r => if (r < 0) -r else 0)

    // Handle edge case where there are no losses
    if (losses.sum == 0) return 100.0

    val avgGain = gains.takeRight(math.min(periods, gains.length)).sum / math.min(periods, gains.length)
    val avgLoss = losses.takeRight(math.min(periods, losses.length)).sum / math.min(periods, losses.length)

    // Prevent division by zero
    if (avgLoss == 0) return 100.0

    val rs = avgGain / avgLoss
    100 - (100 / (1 + rs))
  }

  // Calculate EMA
  def calculateEMA(prices: List[Double], period: Int): Double = {
    if (prices.length < period) return prices.lastOption.getOrElse(0.0)

    val k = 2.0 / (period + 1.0)
    val initialEMA = prices.take(period).sum / period

    prices.drop(period).foldLeft(initialEMA) { (ema, price) =>
      price * k + ema * (1 - k)
    }
  }

  // Calculate MACD
  def calculateMACD(closePrices: List[Double]): (Double, Double, Double) = {
    if (closePrices.length < 26) {
      return (0.0, 0.0, 0.0) // Not enough data
    }

    val shortEMA = calculateEMA(closePrices, 12)
    val longEMA = calculateEMA(closePrices, 26)
    val macd = shortEMA - longEMA

    // Create a list of MACD values (simplified for this example)
    val macdList = List.fill(9)(macd)
    val signalLine = calculateEMA(macdList, 9)
    val macdHistogram = macd - signalLine

    (macd, signalLine, macdHistogram)
  }

  // Interpret RSI value
  def interpretRSI(rsi: Double): String = {
    rsi match {
      case r if r > 70 => "Overbought - potential reversal or correction likely"
      case r if r > 60 => "Approaching overbought - momentum may be slowing"
      case r if r < 30 => "Oversold - potential bullish reversal opportunity"
      case r if r < 40 => "Approaching oversold - watch for buying opportunities"
      case r if r >= 45 && r <= 55 => "Neutral - no strong directional signal"
      case r if r > 55 => "Bullish momentum - trend likely continuing upward"
      case r if r < 45 => "Bearish momentum - trend likely continuing downward"
    }
  }

  // Interpret MACD value
  def interpretMACD(macd: Double, signalLine: Double, histogram: Double): String = {
    (macd, signalLine, histogram) match {
      case (m, s, h) if m > s && h > 0 && h.abs > m.abs * 0.1 => "Strong bullish momentum - positive crossover"
      case (m, s, h) if m < s && h < 0 && h.abs > m.abs * 0.1 => "Strong bearish momentum - negative crossover"
      case (m, s, _) if m > s => "Bullish - MACD above signal line"
      case (m, s, _) if m < s => "Bearish - MACD below signal line"
      case (m, s, _) if m == s => "Neutral - watch for developing trend"
      case (m, _, _) if m > 0 => "Positive momentum - generally bullish"
      case (m, _, _) if m < 0 => "Negative momentum - generally bearish"
      case _ => "Neutral"
    }
  }

  // Interpret volatility
  def interpretVolatility(volatility: Double, assetType: String): String = {
    val (lowThreshold, mediumThreshold, highThreshold) = assetType match {
      case "Cryptocurrency" => (0.01, 0.03, 0.05)
      case _ => (0.005, 0.015, 0.025) // Stocks
    }

    volatility match {
      case v if v >= highThreshold => s"Extremely high - significant risk and potential for large price swings"
      case v if v >= mediumThreshold => s"High - above average volatility indicates uncertainty"
      case v if v >= lowThreshold => s"Moderate - typical market volatility"
      case v => s"Low - price movement is relatively stable"
    }
  }

  // Generate investment recommendation and signals
  def generateAnalysis(
                        rsi: Double,
                        macd: Double,
                        signalLine: Double,
                        macdHistogram: Double,
                        volatility: Double,
                        assetType: String
                      ): (String, Map[String, String]) = {

    // Generate signals for different time horizons
    val shortTermSignal = (rsi, macd, signalLine) match {
      case (r, m, s) if r < 30 && m > s => "Strong Buy"
      case (r, m, s) if r < 40 && m > s => "Buy"
      case (r, m, s) if r > 70 && m < s => "Strong Sell"
      case (r, m, s) if r > 60 && m < s => "Sell"
      case (_, m, s) if m > s && macdHistogram > 0 => "Light Buy"
      case (_, m, s) if m < s && macdHistogram < 0 => "Light Sell"
      case _ => "Neutral"
    }

    val mediumTermSignal = (rsi, volatility) match {
      case (r, v) if r < 40 && v > 0.02 => "Cautious Buy"
      case (r, _) if r < 40 => "Accumulate"
      case (r, v) if r > 65 && v > 0.02 => "Cautious Sell"
      case (r, _) if r > 65 => "Reduce"
      case _ => "Hold"
    }

    val signals = Map(
      "shortTerm" -> shortTermSignal,
      "mediumTerm" -> mediumTermSignal,
      "overallTrend" -> (if (macd > 0) "Bullish" else if (macd < 0) "Bearish" else "Neutral")
    )

    // Generate detailed recommendation
    val baseRecommendation = shortTermSignal match {
      case "Strong Buy" => "Strong Buy - Oversold conditions with positive momentum indicate a potential reversal"
      case "Buy" => "Buy - Technical indicators suggest favorable entry point"
      case "Strong Sell" => "Strong Sell - Overbought conditions with negative momentum suggest exiting positions"
      case "Sell" => "Sell - Technical indicators suggest taking profits or reducing exposure"
      case "Light Buy" => "Light Buy - Positive momentum but watch for confirmation"
      case "Light Sell" => "Light Sell - Negative momentum but watch for confirmation"
      case "Neutral" => "Hold - No strong signals in either direction"
    }

    // Add volatility context
    val volatilityContext = assetType match {
      case "Cryptocurrency" =>
        if (volatility > 0.03) " High volatility suggests using smaller position sizes."
        else if (volatility < 0.01) " Low volatility could indicate a potential breakout soon."
        else ""
      case _ => // Stocks
        if (volatility > 0.02) " Elevated market volatility - consider hedging strategies."
        else if (volatility < 0.005) " Low volatility environment - favorable for systematic strategies."
        else ""
    }

    (baseRecommendation + volatilityContext, signals)
  }

  val routes: Route = {
    pathPrefix("analyse") {
      path(Segment) { symbol =>
        get {
          onComplete(fetchMarketData(symbol.toUpperCase)) {
            case Success((jsonData, function, assetType)) =>
              try {
                val (lastRefreshed, priceMap) = extractPriceData(jsonData, function)
                val closePrices = priceMap.map(_._2)

                if (closePrices.length < 30) {
                  complete(StatusCodes.BadRequest -> "Not enough historical data available for analysis")
                } else {
                  val returns = calculateReturns(closePrices)
                  val volatility = calculateVolatility(returns)
                  val rsi = calculateRSI(returns)
                  val (macd, signalLine, macdHistogram) = calculateMACD(closePrices)

                  val currentPrice = closePrices.last
                  val previousPrice = closePrices.dropRight(1).lastOption.getOrElse(currentPrice)
                  val priceChange = currentPrice - previousPrice
                  val priceChangePercent = if (previousPrice > 0) (priceChange / previousPrice) * 100 else 0

                  val historicalPrices = priceMap.takeRight(30).map { case (date, price) =>
                    PriceData(date, Math.round(price * 100) / 100.0)
                  }

                  val (recommendation, signals) = generateAnalysis(rsi, macd, signalLine, macdHistogram, volatility, assetType)

                  val technicalAnalysis = TechnicalAnalysis(
                    rsi = MetricInfo(
                      Math.round(rsi * 10) / 10.0,
                      interpretRSI(rsi),
                      Map("oversold" -> 30.0, "neutral" -> 50.0, "overbought" -> 70.0)
                    ),
                    macd = MetricInfo(
                      Math.round(macd * 100) / 100.0,
                      interpretMACD(macd, signalLine, macdHistogram),
                      Map("signal" -> Math.round(signalLine * 100) / 100.0, "histogram" -> Math.round(macdHistogram * 100) / 100.0)
                    ),
                    volatility = MetricInfo(
                      Math.round(volatility * 10000) / 10000.0,
                      interpretVolatility(volatility, assetType),
                      assetType match {
                        case "Cryptocurrency" => Map("low" -> 0.01, "medium" -> 0.03, "high" -> 0.05)
                        case _ => Map("low" -> 0.005, "medium" -> 0.015, "high" -> 0.025)
                      }
                    )
                  )

                  val analysis = MarketAnalysis(
                    symbol = symbol.toUpperCase,
                    assetType = assetType,
                    lastRefreshed = lastRefreshed,
                    currentPrice = Math.round(currentPrice * 100) / 100.0,
                    priceChange = Math.round(priceChange * 100) / 100.0,
                    priceChangePercent = Math.round(priceChangePercent * 100) / 100.0,
                    historicalPrices = historicalPrices,
                    technicalAnalysis = technicalAnalysis,
                    recommendation = recommendation,
                    signals = signals
                  )

                  // Enable CORS for frontend access
                  respondWithHeaders(
                    headers.`Access-Control-Allow-Origin`.*,
                    headers.`Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.OPTIONS)
                  ) {
                    complete(StatusCodes.OK -> analysis)
                  }
                }
              } catch {
                case ex: Exception =>
                  complete(StatusCodes.InternalServerError -> s"Error processing data: ${ex.getMessage}")
              }

            case Failure(ex) =>
              complete(StatusCodes.InternalServerError -> s"Failed to analyze ${symbol}: ${ex.getMessage}")
          }
        } ~
          options {
            // Handle CORS preflight requests
            respondWithHeaders(
              headers.`Access-Control-Allow-Origin`.*,
              headers.`Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.OPTIONS),
              headers.`Access-Control-Allow-Headers`("Content-Type")
            ) {
              complete(StatusCodes.OK)
            }
          }
      }
    }
  }
}