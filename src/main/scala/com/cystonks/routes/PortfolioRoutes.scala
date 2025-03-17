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

import com.cystonks.models.{Portfolio, Portfolios}
import com.cystonks.actors.portfolio.PortfolioRegistry
import com.cystonks.actors.portfolio.PortfolioRegistry._
import com.cystonks.models.json.PortfolioJsonFormats._

class PortfolioRoutes(portfolioRegistry: ActorRef[PortfolioRegistry.Command])(implicit val system: ActorSystem[_]) {
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("cy-stonks.routes.ask-timeout"))

  val portfolioRoutes: Route =
    cors() {
      handleExceptions(exceptionHandler) {
        pathPrefix("portfolios") {
          concat(
            pathEndOrSingleSlash {
              concat(
                get {
                  complete(getPortfolios())
                },

                post {
                  entity(as[Portfolio]) { portfolio =>
                    onSuccess(createPortfolio(portfolio)) { performed =>
                      complete((StatusCodes.Created, performed))
                    }
                  }
                }
              )
            },

            path("hello") {
              get {
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Hello from portfolios routes!</h1>"))
              }
            },

            path(Segment) { portfolioId =>
              concat(
                get {
                  rejectEmptyResponse {
                    val portfolioIdInt = portfolioId.toInt
                    onSuccess(getPortfolio(portfolioIdInt)) { response =>
                      complete(response.maybePortfolio)
                    }
                  }
                },

                delete {
                  val portfolioIdInt = portfolioId.toInt
                  onSuccess(deletePortfolio(portfolioIdInt)) { performed =>
                    complete((StatusCodes.OK, performed))
                  }
                }
              )
            }
          )
        }
      }
    }

  def getPortfolios(): Future[Portfolios] =
    portfolioRegistry.ask(GetPortfolios.apply)

  def getPortfolio(portfolioId: Int): Future[GetPortfolioResponse] =
    portfolioRegistry.ask(GetPortfolio(portfolioId, _))

  def createPortfolio(portfolio: Portfolio): Future[ActionPerformed] =
    portfolioRegistry.ask(CreatePortfolio(portfolio, _))

  def deletePortfolio(portfolioId: Int): Future[ActionPerformed] =
    portfolioRegistry.ask(DeletePortfolio(portfolioId, _))

  def exceptionHandler = ExceptionHandler {
    case ex: Exception =>
      complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
  }
}
