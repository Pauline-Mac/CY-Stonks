package com.cystonks.routes

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.Route

import akka.util.Timeout

import scala.concurrent.Future

import com.cystonks.models.{User, Users}
import com.cystonks.actors.user.UserRegistry
import com.cystonks.actors.user.UserRegistry._
import com.cystonks.models.json.UserJsonFormats._

class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("cy-stonks.routes.ask-timeout"))

  val userRoutes: Route =
    cors() {
      handleExceptions(exceptionHandler) {
        pathPrefix("users") {
          concat(
            pathEndOrSingleSlash {
              concat(
                get {
                  complete(getUsers())
                },

                post {
                  entity(as[User]) { user =>
                    onSuccess(createUser(user)) { performed =>
                      complete((StatusCodes.Created, performed))
                    }
                  }
                }
              )
            },

            path("hello") {
              get {
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Hello from Akka HTTP!</h1>"))
              }
            },

            path(Segment) { uuid =>
              concat(
                get {
                  rejectEmptyResponse {
                    onSuccess(getUser(uuid)) { response =>
                      complete(response.maybeUser)
                    }
                  }
                },

                delete {
                  onSuccess(deleteUser(uuid)) { performed =>
                    complete((StatusCodes.OK, performed))
                  }
                }
              )
            }
          )
        }
      }
    }

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers.apply)

  def getUser(uuid: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(uuid, _))

  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))

  def deleteUser(uuid: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(uuid, _))

  def exceptionHandler = ExceptionHandler {
    case ex: Exception =>
      complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
  }
}
