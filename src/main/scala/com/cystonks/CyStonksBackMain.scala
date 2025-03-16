package com.cystonks

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.Terminated
import akka.actor.typed.scaladsl.Behaviors

import java.net.URLEncoder
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

import com.cystonks.actors.httpserver.HttpServer
import com.cystonks.actors.httpservermanager.HttpServerManager

object Main {
  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val httpServerManager = context.spawn(HttpServerManager(), "ServerManager")
      val httpServer = context.spawn(HttpServer(), "HTTPServer1")
      context.watch(httpServer)
      httpServerManager ! HttpServerManager.GetSession("HTTPServer1", httpServer)

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          context.system.log.info("HTTPServer terminated")
          Behaviors.stopped
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