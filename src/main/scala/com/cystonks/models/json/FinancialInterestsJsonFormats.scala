package com.cystonks.actors.financialinterest

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import com.cystonks.models.{FinancialInterest, FinancialInterests}
import com.cystonks.actors.financialinterest.FinancialInterestRegistry

object FinancialInterestJsonFormats extends DefaultJsonProtocol {
  implicit val financialInterestJsonFormat: RootJsonFormat[FinancialInterest] = jsonFormat4(FinancialInterest)
  implicit val financialInterestsJsonFormat: RootJsonFormat[FinancialInterests] = jsonFormat1(FinancialInterests)
  implicit val actionPerformedJsonFormat: RootJsonFormat[FinancialInterestRegistry.ActionPerformed] = jsonFormat1(FinancialInterestRegistry.ActionPerformed)
}
