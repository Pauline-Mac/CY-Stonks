package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import java.time.LocalDateTime

object AssetActor {

  // Définition des commandes que l'acteur peut recevoir
  sealed trait Command
  case class UpdateCurrentValue(newValue: BigDecimal) extends Command
  case class GetAsset(replyTo: ActorRef[Asset]) extends Command

  // Modèle de l'asset
  case class Asset(
                    assetId: Option[Int],  // Peut être None si l'asset vient d'être créé
                    portfolioId: Int,
                    assetType: String,
                    assetSymbol: String,
                    quantity: BigDecimal,
                    purchasePrice: BigDecimal,
                    purchaseDate: LocalDateTime,
                    currentValue: BigDecimal
                  )

  def apply(
             assetId: Option[Int],
             portfolioId: Int,
             assetType: String,
             assetSymbol: String,
             quantity: BigDecimal,
             purchasePrice: BigDecimal,
             purchaseDate: LocalDateTime,
             currentValue: BigDecimal
           ): Behavior[Command] = Behaviors.setup { context =>

    // Initialisation de l'asset
    var asset = Asset(assetId, portfolioId, assetType, assetSymbol, quantity, purchasePrice, purchaseDate, currentValue)
    context.log.info(s"AssetActor créé pour ${asset.assetSymbol} dans le portfolio $portfolioId")

    Behaviors.receiveMessage {
      case UpdateCurrentValue(newValue) =>
        asset = asset.copy(currentValue = newValue)
        context.log.info(s"Valeur actuelle de ${asset.assetSymbol} mise à jour : $newValue")
        Behaviors.same

      case GetAsset(replyTo) =>
        replyTo ! asset
        Behaviors.same
    }
  }
}
