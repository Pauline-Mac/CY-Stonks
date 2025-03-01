package actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import controllers.DataBaseController
import java.time.LocalDateTime

object UserActor {

  sealed trait Command
  final case class CreateUser(name: String, email: String, passwordHash: String, replyTo: ActorRef[Boolean]) extends Command
  final case class AddAsset(assetType: String, assetSymbol: String, quantity: BigDecimal, purchasePrice: BigDecimal, purchaseDate: LocalDateTime, currentValue: BigDecimal) extends Command
  final case class GetPortfolio(replyTo: ActorRef[List[AssetActor.Asset]]) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    var portfolioActor: Option[ActorRef[PortfolioActor.Command]] = None
    var userId: Option[Int] = None

    Behaviors.receiveMessage {
      case CreateUser(name, email, passwordHash, replyTo) =>
        // Création de l'utilisateur dans la base de données et récupération de son ID
        DataBaseController.insertUser(name, email, passwordHash) match {
          case Some(newUserId) =>
            userId = Some(newUserId)
            // Création du portfolio pour l'utilisateur
            DataBaseController.createPortfolio(newUserId, "Premier portfolio") match {
              case Some(portfolioId) =>
                // Création d'un acteur Portfolio pour cet utilisateur
                val portfolioRef = context.spawn(PortfolioActor(portfolioId), s"portfolioActor-$portfolioId")
                portfolioActor = Some(portfolioRef)
                replyTo ! true
                context.log.info(s"Utilisateur $newUserId créé avec portfolio $portfolioId.")
              case None =>
                replyTo ! false
                context.log.error(s"Échec de la création du portfolio pour l'utilisateur $newUserId.")
            }
          case None =>
            replyTo ! false
            context.log.error("Échec de la création de l'utilisateur.")
        }
        Behaviors.same

      case AddAsset(assetType, assetSymbol, quantity, purchasePrice, purchaseDate, currentValue) =>
        portfolioActor match {
          case Some(ref) =>
            ref ! PortfolioActor.AddAsset(None, assetType, assetSymbol, quantity, purchasePrice, purchaseDate, currentValue)
          case None =>
            context.log.error("Impossible d'ajouter un actif, aucun portfolio associé à cet utilisateur.")
        }
        Behaviors.same

      case GetPortfolio(replyTo) =>
        portfolioActor match {
          case Some(ref) => ref ! PortfolioActor.GetPortfolio(replyTo)
          case None => context.log.error("Impossible de récupérer le portefeuille, aucun portfolio associé.")
        }
        Behaviors.same
    }
  }
}
