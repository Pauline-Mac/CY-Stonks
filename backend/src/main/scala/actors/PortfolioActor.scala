package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import java.time.LocalDateTime

object PortfolioActor {
  sealed trait Command
  case class AddAsset(assetId: Option[Int], assetType: String, assetSymbol: String, quantity: BigDecimal,
                      purchasePrice: BigDecimal, purchaseDate: LocalDateTime, currentValue: BigDecimal) extends Command
  case class GetPortfolio(replyTo: ActorRef[List[AssetActor.Asset]]) extends Command
  case class UpdateAssetValue(assetSymbol: String, newValue: BigDecimal) extends Command
  case class GetAllAssets(replyTo: ActorRef[List[AssetActor.Asset]]) extends Command

  // Create a response collector to handle individual asset replies
  private case class CollectAssetResponses(assets: List[AssetActor.Asset], remaining: Int,
                                           originalReplyTo: ActorRef[List[AssetActor.Asset]]) extends Command

  def apply(portfolioId: Int): Behavior[Command] = Behaviors.setup { context =>
    var assets = Map.empty[String, ActorRef[AssetActor.Command]]

    Behaviors.receiveMessage {
      case AddAsset(assetId, assetType, assetSymbol, quantity, purchasePrice, purchaseDate, currentValue) =>
        val assetActor = context.spawn(AssetActor(assetId, portfolioId, assetType, assetSymbol, quantity,
          purchasePrice, purchaseDate, currentValue), s"asset-$assetSymbol")
        assets += (assetSymbol -> assetActor)
        context.log.info(s"Ajout de l'actif $assetSymbol au portfolio $portfolioId")
        Behaviors.same

      case GetPortfolio(replyTo) =>
        if (assets.isEmpty) {
          replyTo ! List.empty[AssetActor.Asset]
        } else {
          // Create a collector to gather all asset responses
          val collector = context.spawn(
            assetCollector(List.empty, assets.size, replyTo),
            s"asset-collector-${System.currentTimeMillis()}"
          )

          // Request each asset to send its data to our collector
          assets.values.foreach { assetActor =>
            // Create an adapter that can receive Asset responses and forward to our collector
            val assetAdapter = context.messageAdapter[AssetActor.Asset] { asset =>
              CollectAssetResponses(List(asset), 1, replyTo)
            }
            assetActor ! AssetActor.GetAsset(assetAdapter)
          }
        }
        Behaviors.same

      case UpdateAssetValue(assetSymbol, newValue) =>
        assets.get(assetSymbol).foreach(_ ! AssetActor.UpdateCurrentValue(newValue))
        Behaviors.same

      case GetAllAssets(replyTo) =>
        // Same pattern as GetPortfolio
        if (assets.isEmpty) {
          replyTo ! List.empty[AssetActor.Asset]
        } else {
          val collector = context.spawn(
            assetCollector(List.empty, assets.size, replyTo),
            s"all-assets-collector-${System.currentTimeMillis()}"
          )

          assets.values.foreach { assetActor =>
            val assetAdapter = context.messageAdapter[AssetActor.Asset] { asset =>
              CollectAssetResponses(List(asset), 1, replyTo)
            }
            assetActor ! AssetActor.GetAsset(assetAdapter)
          }
        }
        Behaviors.same

      case CollectAssetResponses(receivedAssets, remaining, originalReplyTo) =>
        // Internal message for collecting responses
        context.log.debug(s"Received ${receivedAssets.size} assets, $remaining remaining")
        // Not implemented here - this would be handled by the assetCollector behavior
        Behaviors.same
    }
  }

  // Helper behavior for collecting asset responses
  private def assetCollector(
                              collectedAssets: List[AssetActor.Asset],
                              remaining: Int,
                              replyTo: ActorRef[List[AssetActor.Asset]]
                            ): Behavior[Command] = {
    Behaviors.receiveMessage {
      case CollectAssetResponses(receivedAssets, _, _) =>
        val newCollectedAssets = collectedAssets ++ receivedAssets
        val newRemaining = remaining - receivedAssets.size

        if (newRemaining <= 0) {
          // We've collected all assets, send the complete list to the original requester
          replyTo ! newCollectedAssets
          Behaviors.stopped
        } else {
          // Continue collecting
          assetCollector(newCollectedAssets, newRemaining, replyTo)
        }

      case _ => Behaviors.same // Ignore other messages
    }
  }
}