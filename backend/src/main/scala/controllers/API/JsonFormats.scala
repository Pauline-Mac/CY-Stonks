package controllers.API

import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import controllers._

trait JsonFormats extends SprayJsonSupport with DefaultJsonProtocol {
  // Define implicit JSON formats for your case classes
  implicit val assetFormat: RootJsonFormat[Asset] = jsonFormat2(Asset.apply)
  implicit val portfolioResponseFormat: RootJsonFormat[PortfolioResponse] = jsonFormat1(PortfolioResponse.apply)
  implicit val healthResponseFormat: RootJsonFormat[HealthResponse] = jsonFormat2(HealthResponse.apply)

  // For Option[Double] fields
  implicit val stockPriceResponseFormat: RootJsonFormat[StockPriceResponse] = jsonFormat2(StockPriceResponse.apply)
  implicit val rsiResponseFormat: RootJsonFormat[RSIResponse] = jsonFormat2(RSIResponse.apply)
}