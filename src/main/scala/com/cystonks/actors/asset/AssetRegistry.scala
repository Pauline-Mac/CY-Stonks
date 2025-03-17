package com.cystonks.actors.asset

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.collection.immutable
import com.cystonks.models.{Asset, Assets}

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
    Behaviors.receiveMessage {
      case GetAssets(replyTo) =>
        replyTo ! Assets(assets.toSeq)
        Behaviors.same

      case CreateAsset(asset, replyTo) =>
        replyTo ! ActionPerformed(s"Asset ${asset.assetId} created.")
        registry(assets + asset)

      case GetAsset(assetId, replyTo) =>
        replyTo ! GetAssetResponse(assets.find(_.assetId == assetId))
        Behaviors.same

      case DeleteAsset(assetId, replyTo) =>
        replyTo ! ActionPerformed(s"Asset $assetId deleted.")
        registry(assets.filterNot(_.assetId == assetId))
    }
}
