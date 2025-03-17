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

import com.cystonks.models.{Asset, Assets}
import com.cystonks.actors.asset.AssetRegistry
import com.cystonks.actors.asset.AssetRegistry._
import com.cystonks.models.json.AssetJsonFormats._

class AssetRoutes(assetRegistry: ActorRef[AssetRegistry.Command])(implicit val system: ActorSystem[_]) {
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("cy-stonks.routes.ask-timeout"))

  val assetRoutes: Route =
    cors() {
      handleExceptions(exceptionHandler) {
        pathPrefix("assets") {
          concat(
            pathEndOrSingleSlash {
              concat(
                get {
                  complete(getAssets())
                },

                post {
                  entity(as[Asset]) { asset =>
                    onSuccess(createAsset(asset)) { performed =>
                      complete((StatusCodes.Created, performed))
                    }
                  }
                }
              )
            },

            path("hello") {
              get {
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Hello from assets routes!</h1>"))
              }
            },

            path(Segment) { assetId =>
              concat(
                get {
                  rejectEmptyResponse {
                    val assetIdInt = assetId.toInt
                    onSuccess(getAsset(assetIdInt)) { response =>
                      complete(response.maybeAsset)
                    }
                  }
                },

                delete {
                  val assetIdInt = assetId.toInt
                  onSuccess(deleteAsset(assetIdInt)) { performed =>
                    complete((StatusCodes.OK, performed))
                  }
                }
              )
            }
          )
        }
      }
    }

  def getAssets(): Future[Assets] =
    assetRegistry.ask(GetAssets.apply)

  def getAsset(assetId: Int): Future[GetAssetResponse] =
    assetRegistry.ask(GetAsset(assetId, _))

  def createAsset(asset: Asset): Future[ActionPerformed] =
    assetRegistry.ask(CreateAsset(asset, _))

  def deleteAsset(assetId: Int): Future[ActionPerformed] =
    assetRegistry.ask(DeleteAsset(assetId, _))

  def exceptionHandler = ExceptionHandler {
    case ex: Exception =>
      complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
  }
}
