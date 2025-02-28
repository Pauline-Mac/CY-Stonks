package controllers

// Define your case classes here
case class Asset(symbol: String, quantity: Double)
case class PortfolioResponse(assets: List[Asset])
case class HealthResponse(status: String, message: String)
case class StockPriceResponse(symbol: String, price: Option[Double])
case class RSIResponse(symbol: String, value: Option[Double])
