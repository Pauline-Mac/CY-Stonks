package com.cystonks.actors.user

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import slick.jdbc.PostgresProfile.api._
import com.cystonks.config.DatabaseConfig._
import com.cystonks.models.{User, Users, Portfolio}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object UserRegistry {
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUser(uuid: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(uuid: String, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUserPortfolios(userId: String, replyTo: ActorRef[GetUserPortfoliosResponse]) extends Command

  final case class GetUserResponse(maybeUser: Option[User])
  final case class GetUserPortfoliosResponse(portfolios: Seq[Portfolio])
  final case class ActionPerformed(description: String)

  // Message interne pour mettre à jour le registre
  private final case class UpdateRegistry(user: User) extends Command

  // Passer l'ExecutionContext explicitement
  def apply()(implicit ec: ExecutionContext): Behavior[Command] = registry(Set.empty)

  private def registry(users: Set[User])(implicit ec: ExecutionContext): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case GetUsers(replyTo) =>
          replyTo ! Users(users.toSeq)
          Behaviors.same

        case CreateUser(user, replyTo) =>
          val insertUserAction =
            sqlu"""
              INSERT INTO users (user_id, username, password_hash)
              VALUES (uuid(${user.uuid}), ${user.username}, ${user.password})
              ON CONFLICT (user_id) DO NOTHING
            """

          val insertFuture: Future[Int] = db.run(insertUserAction)

          insertFuture.onComplete {
            case Success(rowsAffected) =>
              if (rowsAffected > 0) {
                replyTo ! ActionPerformed(s"User ${user.uuid} created.")
                context.self ! UpdateRegistry(user)
              } else {
                replyTo ! ActionPerformed(s"User ${user.uuid} already exists.")
              }

            case Failure(ex) =>
              replyTo ! ActionPerformed(s"Failed to create user ${user.uuid}: ${ex.getMessage}")
          }

          Behaviors.same

        case UpdateRegistry(user) =>
          registry(users + user)

        case GetUser(uuid, replyTo) =>
          replyTo ! GetUserResponse(users.find(_.uuid == uuid))
          Behaviors.same

        case DeleteUser(uuid, replyTo) =>
          val deleteUserAction =
            sqlu"""
              DELETE FROM users
              WHERE user_id = uuid($uuid)
            """


          val deleteFuture: Future[Int] = db.run(deleteUserAction)

          deleteFuture.onComplete {
            case Success(rowsAffected) =>
              if (rowsAffected > 0) {
                replyTo ! ActionPerformed(s"User $uuid deleted.")
                context.self ! UpdateRegistry(User(uuid, "", "", Seq.empty, Seq.empty))
              } else {
                replyTo ! ActionPerformed(s"User $uuid not found.")
              }

            case Failure(ex) =>
              replyTo ! ActionPerformed(s"Failed to delete user $uuid: ${ex.getMessage}")
          }

          Behaviors.same

        case GetUserPortfolios(userId, replyTo) =>
          val getUserPortfoliosAction =
            sql"""
              SELECT portfolio_id, user_id, portfolio_name
              FROM portfolios
              WHERE user_id = uuid($userId)
            """.as[(Int, String, String)]


          val portfoliosFuture: Future[Seq[Portfolio]] = db.run(getUserPortfoliosAction).map { tuples =>
            tuples.map { case (portfolioId, userId, portfolioName) =>
              Portfolio(portfolioId, userId, portfolioName)
            }
          }


          portfoliosFuture.onComplete {
            case Success(portfolios) =>
              replyTo ! GetUserPortfoliosResponse(portfolios)

            case Failure(ex) =>
              replyTo ! GetUserPortfoliosResponse(Seq.empty)
          }

          Behaviors.same
      }
    }
}