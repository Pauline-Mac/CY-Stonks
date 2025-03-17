package com.cystonks.models

import scala.collection.immutable

final case class Portfolio(uuid: String, user_uuid: String, name: String)
final case class Portfolios(Portfolios: immutable.Seq[Portfolio])