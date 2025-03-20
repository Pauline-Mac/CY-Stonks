package com.cystonks.config

import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext

object DatabaseConfig {
  private val config = ConfigFactory.load()
  private val dbConfig = config.getConfig("slick.dbs.default.db")

  val dbUrl = dbConfig.getString("url")
  val dbUser = dbConfig.getString("user")
  val dbPassword = dbConfig.getString("password")
  val dbDriver = dbConfig.getString("driver")

  val db: Database = Database.forURL(dbUrl, driver = dbDriver, user = dbUser, password = dbPassword)
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
}
