package com.cystonks

import com.typesafe.config.ConfigFactory
import slick.jdbc.{GetResult, PostgresProfile}
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import java.util.UUID
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.cystonks.actors.httpserver.HttpServer
import com.cystonks.actors.httpservermanager.HttpServerManager
import com.cystonks.config.DatabaseConfig.db

object Main {

  private val config = ConfigFactory.load()

  private val dbConfig = config.getConfig("slick.dbs.default.db")
  println(s"Database URL: ${dbConfig.getString("url")}")
  println(s"Database User: ${dbConfig.getString("user")}")
  println(s"Database Password: ${dbConfig.getString("password")}")
  println(s"Database Driver: ${dbConfig.getString("driver")}")

  testDatabaseConnection()

  def apply(): Behavior[akka.NotUsed] =
    Behaviors.setup { context =>
      val httpServerManager = context.spawn(HttpServerManager(), "ServerManager")
      val httpServer = context.spawn(HttpServer(), "HTTPServer1")
      context.watch(httpServer)
      httpServerManager ! HttpServerManager.GetSession("HTTPServer1", httpServer)

      Behaviors.receiveSignal {
        case (_, akka.actor.typed.Terminated(_)) =>
          context.system.log.info("HTTPServer terminated")
          Behaviors.stopped
      }
    }

  def testDatabaseConnection(): Unit = {
    val testQuery = sql"SELECT 1".as[Int]

    db.run(testQuery).map { result =>
      println(s"Database connection successful! Result: $result")

      try {
        val testUserId = UUID.randomUUID()
        val testUserIdStr = testUserId.toString

        val insertUserAction =
          sqlu"""
                INSERT INTO users (user_id, username, password_hash)
                VALUES (uuid($testUserIdStr), 'test_user', 'hashed_password_placeholder')
                ON CONFLICT (username) DO NOTHING
              """

        val insertFuture = db.run(insertUserAction)
        Await.result(insertFuture, 5.seconds)
        println("Attempted to add a test user")

        implicit val getUUID: GetResult[UUID] = GetResult(r => r.nextObject().asInstanceOf[UUID])
        implicit val getUUIDStringTuple: GetResult[(UUID, String)] = GetResult(r =>
          (r.nextObject().asInstanceOf[UUID], r.nextString())
        )

        val usersFuture = db.run(sql"""SELECT user_id, username FROM users""".as[(UUID, String)])
        val users = Await.result(usersFuture, 5.seconds)
        println("=== USERS TABLE ===")
        if (users.isEmpty) println("No users found")
        else users.foreach(user => println(s"User ID: ${user._1}, Username: ${user._2}"))

        val portfoliosFuture = db.run(sql"""SELECT portfolio_id, user_id, portfolio_name FROM portfolios""".as[(Int, UUID, String)])
        val portfolios = Await.result(portfoliosFuture, 5.seconds)
        println("=== PORTFOLIOS TABLE ===")
        if (portfolios.isEmpty) println("No portfolios found")
        else portfolios.foreach(p => println(s"Portfolio ID: ${p._1}, User ID: ${p._2}, Name: ${p._3}"))

        val assetsFuture = db.run(sql"""SELECT asset_id, portfolio_id, asset_type, asset_symbol, quantity, purchase_price FROM assets""".as[(Int, Int, String, String, BigDecimal, BigDecimal)])
        val assets = Await.result(assetsFuture, 5.seconds)
        println("=== ASSETS TABLE ===")
        if (assets.isEmpty) println("No assets found")
        else assets.foreach(a => println(s"Asset ID: ${a._1}, Portfolio ID: ${a._2}, Type: ${a._3}, Symbol: ${a._4}, Quantity: ${a._5}, Purchase Price: ${a._6}"))

        println("All database operations completed successfully")
      } catch {
        case e: Exception => println(s"Error during database operations: ${e.getMessage}")
      }
    }.recover {
      case exception => println(s"Initial database connection failed: ${exception.getMessage}")
    }
  }

  def main(args: Array[String]): Unit = {
    ActorSystem(Main(), "CyStonksBack")
  }
}


// WIP
// private def alphavantage()(implicit system: ActorSystem[_]): Unit = {
//   import system.executionContext
//   import scala.concurrent.duration._
// import akka.http.scaladsl.Http

//   // Define the API URL
//   val apiUrl = "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol=IBM&interval=5min&apikey=demo"
    
//   // Create a source that emits a single HTTP request
//   // val source = Source.single(HttpRequest(uri = apiUrl))
//   // Create a source that emits a request every 5 minutes
//   val source = Source.tick(0.seconds, 5.minutes, HttpRequest(uri = apiUrl))
  
//   // Execute the HTTP request and process the response
//   val responseStream = source
//     .mapAsync(1)(request => Http().singleRequest(request))
//     .flatMapConcat(response => {
//       response.entity.dataBytes
//         .fold(akka.util.ByteString.empty)(_ ++ _)
//         .map(_.utf8String)
//     })
  
//   // Run the stream and print the result
//   responseStream
//     .runForeach(data => println(s"Received data: $data"))
//     .onComplete(_ => {
//       println("Stream completed")
//       system.terminate()
//     })
// }

// case class UUIDGen(seed: Long) {
//   def nextUUID: (UUID, UUIDGen) = {
//     val newSeed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1)
//     val uuid = new UUID(seed, newSeed)
//     (uuid, UUIDGen(newSeed))
//   }
// }

// val generator = UUIDGen(42L)
// val (uuid1, gen2) = generator.nextUUID
// val (uuid2, _) = gen2.nextUUID

// println(uuid1) // Deterministic UUID
// println(uuid2) // Next deterministic UUID