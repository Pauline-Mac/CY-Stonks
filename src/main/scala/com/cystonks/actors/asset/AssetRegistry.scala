package com.cystonks.actors.asset

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import com.cystonks.models.{Asset, Assets}
import slick.jdbc.PostgresProfile.api._
import com.cystonks.config.DatabaseConfig._
import com.cystonks.config.AlphaVantageConfig
import spray.json._
import spray.json.DefaultJsonProtocol._

object AssetRegistry {
  // Command protocol
  sealed trait Command
  final case class GetAssets(replyTo: ActorRef[Assets]) extends Command
  final case class GetAssetsByPortfolio(portfolioId: Int, replyTo: ActorRef[Assets]) extends Command
  final case class CreateAsset(asset: Asset, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetAsset(assetId: Int, replyTo: ActorRef[GetAssetResponse]) extends Command
  final case class DeleteAsset(assetId: Int, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class UpdateRegistry(assets: Set[Asset]) extends Command
  final case class AddAssetToRegistry(asset: Asset) extends Command

  // Response messages
  final case class GetAssetResponse(maybeAsset: Option[Asset])
  final case class ActionPerformed(description: String)

  // Alpha Vantage API integration
  private object AlphaVantageClient {
    def getLatestPrice(symbol: String)(implicit ec: ExecutionContext, system: akka.actor.typed.ActorSystem[_]): Future[Option[Double]] = {
      val apiKey = AlphaVantageConfig.apiKey
      val request = HttpRequest(
        method = HttpMethods.GET,
        uri = s"https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=$symbol&apikey=$apiKey"
      )

      Http()(system.classicSystem).singleRequest(request).flatMap { response =>
        response.status match {
          case StatusCodes.OK =>
            Unmarshal(response.entity).to[String].map { jsonString =>
              Try {
                val json = jsonString.parseJson.asJsObject
                val globalQuote = json.fields("Global Quote").asJsObject
                val price = globalQuote.fields("05. price").convertTo[String].toDouble
                Some(price)
              }.getOrElse(None)
            }
          case _ =>
            Future.successful(None)
        }
      }.recover {
        case ex =>
          None
      }
    }

    def getCryptoPrices(symbol: String)(implicit ec: ExecutionContext, system: akka.actor.typed.ActorSystem[_]): Future[Option[Double]] = {
      val apiKey = AlphaVantageConfig.apiKey
      val request = HttpRequest(
        method = HttpMethods.GET,
        uri = s"https://www.alphavantage.co/query?function=CURRENCY_EXCHANGE_RATE&from_currency=$symbol&to_currency=USD&apikey=$apiKey"
      )

      Http()(system.classicSystem).singleRequest(request).flatMap { response =>
        response.status match {
          case StatusCodes.OK =>
            Unmarshal(response.entity).to[String].map { jsonString =>
              Try {
                val json = jsonString.parseJson.asJsObject
                val exchangeRate = json.fields("Realtime Currency Exchange Rate").asJsObject
                val price = exchangeRate.fields("5. Exchange Rate").convertTo[String].toDouble
                Some(price)
              }.getOrElse(None)
            }
          case _ =>
            Future.successful(None)
        }
      }.recover {
        case ex =>
          None
      }
    }

    def getPrice(symbol: String, assetType: String)(implicit ec: ExecutionContext, system: akka.actor.typed.ActorSystem[_]): Future[Option[Double]] = {
      assetType.toLowerCase match {
        case "stock" | "etf" => getLatestPrice(symbol)
        case "crypto" => getCryptoPrices(symbol)
        case _ => Future.successful(None)
      }
    }
  }

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(assets: Set[Asset]): Behavior[Command] = Behaviors.setup { context =>
    implicit val ec: ExecutionContext = context.executionContext
    implicit val system = context.system

    // Load initial assets from database
    val initialLoadQuery = sql"""SELECT asset_id, portfolio_id, asset_type, asset_symbol, quantity, purchase_price FROM assets""".as[(Int, Int, String, String, Double, Double)]

    db.run(initialLoadQuery).onComplete {
      case Success(rows) =>
        val loadedAssets = rows.map { case (assetId, portfolioId, assetType, assetSymbol, quantity, purchasePrice) =>
          Asset(assetId, portfolioId, assetType, assetSymbol, quantity, purchasePrice)
        }.toSet
        context.self ! UpdateRegistry(loadedAssets)
      case Failure(ex) =>
        context.log.error("Failed to load initial assets: {}", ex.getMessage)
    }

    Behaviors.receiveMessage {
      case GetAssets(replyTo) =>
        val enrichedAssetsFuture = Future.sequence(
          assets.map { asset =>
            AlphaVantageClient.getPrice(asset.assetSymbol, asset.assetType).map { priceOpt =>
              asset
            }
          }
        )

        enrichedAssetsFuture.onComplete {
          case Success(enrichedAssets) =>
            replyTo ! Assets(enrichedAssets.toSeq)
          case Failure(_) =>
            replyTo ! Assets(assets.toSeq)
        }

        Behaviors.same

      case GetAssetsByPortfolio(portfolioId, replyTo) =>
        val query = sql"""
          SELECT asset_id, portfolio_id, asset_type, asset_symbol, quantity, purchase_price
          FROM assets
          WHERE portfolio_id = $portfolioId
        """.as[(Int, Int, String, String, Double, Double)]

        db.run(query).onComplete {
          case Success(rows) =>
            val portfolioAssets = rows.map { case (assetId, pId, assetType, assetSymbol, quantity, purchasePrice) =>
              Asset(assetId, pId, assetType, assetSymbol, quantity, purchasePrice)
            }

            replyTo ! Assets(portfolioAssets)

            updateMarketDataCache(portfolioAssets)

          case Failure(ex) =>
            context.log.error(s"Failed to get assets for portfolio $portfolioId: ${ex.getMessage}")
            replyTo ! Assets(Seq.empty)
        }
        Behaviors.same

      case CreateAsset(asset, replyTo) =>
        val assetWithPriceFuture = if (asset.purchasePrice <= 0) {
          AlphaVantageClient.getPrice(asset.assetSymbol, asset.assetType).map { priceOpt =>
            asset.copy(purchasePrice = priceOpt.getOrElse(asset.purchasePrice))
          }
        } else {
          Future.successful(asset)
        }

        assetWithPriceFuture.flatMap { finalAsset =>
          val insertAssetAction = sqlu"""
            INSERT INTO assets (asset_id, portfolio_id, asset_type, asset_symbol, quantity, purchase_price)
            VALUES (${finalAsset.assetId}, ${finalAsset.portfolioId}, ${finalAsset.assetType}, ${finalAsset.assetSymbol},
                    ${finalAsset.quantity}, ${finalAsset.purchasePrice})
            ON CONFLICT (asset_id) DO UPDATE SET
              portfolio_id = ${finalAsset.portfolioId},
              asset_type = ${finalAsset.assetType},
              asset_symbol = ${finalAsset.assetSymbol},
              quantity = ${finalAsset.quantity},
              purchase_price = ${finalAsset.purchasePrice}
          """
          db.run(insertAssetAction).map(_ => finalAsset)
        }.onComplete {
          case Success(finalAsset) =>
            replyTo ! ActionPerformed(s"Asset ${finalAsset.assetId} created/updated with purchase price ${finalAsset.purchasePrice}.")
            context.self ! AddAssetToRegistry(finalAsset)
          case Failure(ex) =>
            replyTo ! ActionPerformed(s"Failed to create asset ${asset.assetId}: ${ex.getMessage}")
        }

        Behaviors.same

      case AddAssetToRegistry(asset) =>
        registry(assets + asset)

      case UpdateRegistry(newAssets) =>
        registry(newAssets)

      case GetAsset(assetId, replyTo) =>
        val query = sql"""
          SELECT asset_id, portfolio_id, asset_type, asset_symbol, quantity, purchase_price
          FROM assets
          WHERE asset_id = $assetId
        """.as[(Int, Int, String, String, Double, Double)]

        db.run(query).onComplete {
          case Success(rows) =>
            val maybeAsset = rows.headOption.map { case (id, portfolioId, assetType, assetSymbol, quantity, purchasePrice) =>
              Asset(id, portfolioId, assetType, assetSymbol, quantity, purchasePrice)
            }

            replyTo ! GetAssetResponse(maybeAsset)

            // Update market data in the background
            maybeAsset.foreach { asset =>
              updateMarketDataForAsset(asset)
            }

          case Failure(ex) =>
            context.log.error(s"Failed to get asset $assetId: ${ex.getMessage}")
            replyTo ! GetAssetResponse(None)
        }
        Behaviors.same

      case DeleteAsset(assetId, replyTo) =>
        val deleteAssetAction = sqlu"""DELETE FROM assets WHERE asset_id = $assetId"""

        db.run(deleteAssetAction).onComplete {
          case Success(rowsAffected) =>
            if (rowsAffected > 0) {
              replyTo ! ActionPerformed(s"Asset $assetId deleted.")
              context.self ! UpdateRegistry(assets.filterNot(_.assetId == assetId))
            } else {
              replyTo ! ActionPerformed(s"Asset $assetId not found.")
            }
          case Failure(ex) =>
            replyTo ! ActionPerformed(s"Failed to delete asset $assetId: ${ex.getMessage}")
        }
        Behaviors.same
    }
  }

  // Helper methods for market data caching (for internal use only)
  private def updateMarketDataCache(assets: Seq[Asset])(implicit ec: ExecutionContext, system: akka.actor.typed.ActorSystem[_]): Unit = {
    // Group by symbol to avoid duplicate API calls
    val symbolsToUpdate = assets.map(a => (a.assetSymbol, a.assetType)).distinct

    symbolsToUpdate.foreach { case (symbol, assetType) =>
      AlphaVantageClient.getPrice(symbol, assetType).foreach {
        case Some(price) =>
          // Store the price in market_data table for analytics purposes
          storeMarketData(symbol, price)
        case None => // Price fetch failed, nothing to store
      }
    }
  }

  private def updateMarketDataForAsset(asset: Asset)(implicit ec: ExecutionContext, system: akka.actor.typed.ActorSystem[_]): Unit = {
    AlphaVantageClient.getPrice(asset.assetSymbol, asset.assetType).foreach {
      case Some(price) =>
        storeMarketData(asset.assetSymbol, price)
      case None => // Price fetch failed, nothing to store
    }
  }

  private def storeMarketData(assetSymbol: String, price: Double)(implicit ec: ExecutionContext): Future[Int] = {
    val insertMarketDataAction = sqlu"""
      INSERT INTO market_data (asset_symbol, price, timestamp)
      VALUES ($assetSymbol, $price, now())
    """
    db.run(insertMarketDataAction)
  }
}