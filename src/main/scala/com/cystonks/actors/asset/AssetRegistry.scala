package com.cystonks.actors.asset

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable
import com.cystonks.models.{Asset, Assets}
import slick.jdbc.PostgresProfile.api._
import com.cystonks.config.DatabaseConfig._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AssetRegistry {
  sealed trait Command
  final case class GetAssets(replyTo: ActorRef[Assets]) extends Command
  final case class CreateAsset(asset: Asset, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetAsset(assetId: Int, replyTo: ActorRef[GetAssetResponse]) extends Command
  final case class DeleteAsset(assetId: Int, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetAssetResponse(maybeAsset: Option[Asset])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(assets: Set[Asset]): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val ec: ExecutionContext = context.executionContext

      Behaviors.receiveMessage {
        case GetAssets(replyTo) =>
          replyTo ! Assets(assets.toSeq)
          Behaviors.same

        case CreateAsset(asset, replyTo) =>
          val insertAssetAction =
            sqlu"""
                  INSERT INTO assets (asset_id, portfolio_id, asset_type, asset_symbol, quantity, purchase_price)
                  VALUES (${asset.assetId}, ${asset.portfolioId}, ${asset.assetType}, ${asset.assetSymbol}, ${asset.quantity}, ${asset.purchasePrice})
                  ON CONFLICT (asset_id) DO NOTHING
                """

          val insertFuture: Future[Int] = db.run(insertAssetAction)

          insertFuture.onComplete {
            case Success(_) =>
              replyTo ! ActionPerformed(s"Asset ${asset.assetId} created.")
              context.self ! UpdateRegistry(asset) // Envoie un message interne pour mettre à jour le registre
            case Failure(ex) =>
              replyTo ! ActionPerformed(s"Failed to create asset ${asset.assetId}: ${ex.getMessage}")
          }

          Behaviors.same

        case UpdateRegistry(asset) =>
          registry(assets + asset) // Retourne un nouveau `Behavior` avec le registre mis à jour

        case GetAsset(assetId, replyTo) =>
          replyTo ! GetAssetResponse(assets.find(_.assetId == assetId))
          Behaviors.same

        case DeleteAsset(assetId, replyTo) =>
          replyTo ! ActionPerformed(s"Asset $assetId deleted.")
          registry(assets.filterNot(_.assetId == assetId))
      }
    }

  // Message interne pour mettre à jour l'état du registre
  private final case class UpdateRegistry(asset: Asset) extends Command
}
