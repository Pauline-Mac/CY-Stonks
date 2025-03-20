package com.cystonks.models

import scala.collection.immutable

final case class FinancialInterest(uuid: String, name: String, description: String, endpoint: String)
final case class FinancialInterests(financialInterests: immutable.Seq[FinancialInterest])