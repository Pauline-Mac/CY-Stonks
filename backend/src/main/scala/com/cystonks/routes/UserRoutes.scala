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
import akka.http.scaladsl.server.{ExceptionHandler, Directive1, AuthorizationFailedRejection, Rejection}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Success, Failure}

import com.cystonks.models.{User, Users, LoginRequest, AuthResponse, AuthenticationResponse}
import com.cystonks.actors.user.UserRegistry
import com.cystonks.actors.user.UserRegistry._
import com.cystonks.models.json.UserJsonFormats._
import com.cystonks.utils.JwtUtils

class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("cy-stonks.routes.ask-timeout"))
  import system.executionContext

  val userRoutes: Route = cors() {
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
          },

          path("login") {
            post {
              entity(as[LoginRequest]) { loginRequest =>
                val authFuture = authenticateUser(loginRequest)
                onSuccess(authFuture) { response =>
                  response.token match {
                    case Some(token) =>
                      val authCookie = HttpCookie(
                        name = "auth-token",
                        value = token,
                        secure = true,
                        httpOnly = true,
                        path = Some("/")
                      )
                      setCookie(authCookie) {
                        complete(AuthResponse(token = token, user = response.user.get))
                      }
                    case None =>
                      complete(StatusCodes.Unauthorized -> "Identifiants invalides")
                  }
                }
              }
            }
          },
          path("logout") {
            get {
              deleteCookie("auth-token") {
                complete(StatusCodes.OK -> "Déconnexion réussie")
              }
            }
          },
          path("me") {
            get {
              authenticate { user =>
                complete(user)
              }
            }
          }
        )
      }
    }
  }

  def authenticate: Directive1[User] = {
    optionalCookie("auth-token").flatMap {
      case Some(cookie) =>
        val token = cookie.value
        JwtUtils.validateToken(token) match {
          case Success(claims) =>
            val userUuid = claims.subject.getOrElse("")
            onSuccess(getUser(userUuid)).flatMap { response =>
              response.maybeUser match {
                case Some(user) => provide(user)
                case None =>
                  reject(AuthorizationFailedRejection)
              }
            }
          case Failure(_) =>
            reject(AuthorizationFailedRejection)
        }
      case None =>
        reject(AuthorizationFailedRejection)
    }
  }

  def getUsers(): Future[Users] = userRegistry.ask(GetUsers.apply)
  def getUser(uuid: String): Future[GetUserResponse] = userRegistry.ask(GetUser(uuid, _))
  def createUser(user: User): Future[ActionPerformed] = userRegistry.ask(CreateUser(user, _))
  def deleteUser(uuid: String): Future[ActionPerformed] = userRegistry.ask(DeleteUser(uuid, _))

  def authenticateUser(loginRequest: LoginRequest): Future[AuthenticationResponse] = {
    userRegistry.ask(AuthenticateUser(loginRequest, _))
  }

  def exceptionHandler = ExceptionHandler {
    case ex: Exception => complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
  }
}