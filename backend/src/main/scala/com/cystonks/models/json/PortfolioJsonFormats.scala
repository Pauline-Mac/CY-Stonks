package com.cystonks.models.json

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import com.cystonks.models.{Portfolio, Portfolios}
import com.cystonks.actors.portfolio.PortfolioRegistry

object PortfolioJsonFormats extends DefaultJsonProtocol {
  implicit val portfolioJsonFormat: RootJsonFormat[Portfolio] = jsonFormat3(Portfolio)
  implicit val portfoliosJsonFormat: RootJsonFormat[Portfolios] = jsonFormat1(Portfolios)
  implicit val actionPerformedJsonFormat: RootJsonFormat[PortfolioRegistry.ActionPerformed] = jsonFormat1(PortfolioRegistry.ActionPerformed)
}
