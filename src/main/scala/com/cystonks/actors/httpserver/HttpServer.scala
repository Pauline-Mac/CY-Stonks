package com.cystonks.actors.httpserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Route._

import com.cystonks.actors.httpservermanager.HttpServerManager
import com.cystonks.actors.httpservermanager.HttpServerManager._
import com.cystonks.actors.user.UserRegistry
import com.cystonks.actors.user.UserRegistry._
import com.cystonks.routes.UserRoutes

import scala.util.Failure
import scala.util.Success

object HttpServer {

  def apply(): Behavior[SessionEvent] =
    Behaviors.setup { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      import system.executionContext

      Behaviors.receiveMessage {
        case SessionGranted(handle) =>
          val userRegistry = context.spawn(UserRegistry(), "UserRegistry")
          val routes = new UserRoutes(userRegistry)(context.system)

          // newServerAt(anyIpAdress, port)
          // 0.0.0.0 to be accessible from any IP address
          // localhost or 127.0.0.1 to be accessible only from the same machine
          val futureBinding = Http().newServerAt("0.0.0.0", 8081).bind(routes.userRoutes)

          futureBinding.onComplete {
            case Success(binding) =>
              val address = binding.localAddress
              context.system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)

            case Failure(ex) =>
              context.system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
              context.system.terminate()
          }
          
          handle ! PostMessage("Hello World!")
          Behaviors.same
        case SessionDenied(reason) =>
          context.log.warn(s"Session denied: $reason")
          Behaviors.same
        case MessagePosted(serverName, message) =>
          context.log.info("Message from '{}': {}", serverName, message)
          Behaviors.same
      }
    }
}