package com.cystonks.actors.Portfolio

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import com.cystonks.models.{Portfolio, Portfolios}
import com.cystonks.actors.portfolio.PortfolioRegistry

object PortfolioJsonFormats extends DefaultJsonProtocol {
  implicit val PortfolioJsonFormat: RootJsonFormat[Portfolio] = jsonFormat3(Portfolio)
  implicit val PortfoliosJsonFormat: RootJsonFormat[Portfolios] = jsonFormat1(Portfolios)
  implicit val actionPerformedJsonFormat: RootJsonFormat[PortfolioRegistry.ActionPerformed] = jsonFormat1(PortfolioRegistry.ActionPerformed)
}
