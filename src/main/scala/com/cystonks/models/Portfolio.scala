package com.cystonks.models

import scala.collection.immutable

final case class Portfolio(portfolioId: Int, userUuid: String, name: String)
final case class Portfolios(portfolios: immutable.Seq[Portfolio])
