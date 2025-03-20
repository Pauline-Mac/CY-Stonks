// Example of UserActor content
// User Actor should only manage an individual user session

// import akka.actor.typed.{ActorRef, Behavior}
// import akka.actor.typed.scaladsl.Behaviors

// object UserActor {
//   sealed trait Command

//   // Commands for user actions
//   final case class LogIn(username: String, replyTo: ActorRef[ActionPerformed]) extends Command
//   final case class LogOut(replyTo: ActorRef[ActionPerformed]) extends Command
//   final case class GetStatus(replyTo: ActorRef[UserStatus]) extends Command
//   final case class SendMessage(message: String) extends Command

//   // Actor state
//   sealed trait UserState
//   final case object LoggedOut extends UserState
//   final case class LoggedIn(username: String) extends UserState

//   // Message for confirming actions
//   final case class ActionPerformed(message: String)

//   // Status of the user (could be logged in or logged out)
//   final case class UserStatus(state: UserState)

//   // Actor behavior: managing session state
//   def apply(): Behavior[Command] = manageState(LoggedOut)

//   private def manageState(state: UserState): Behavior[Command] = Behaviors.receiveMessage {
//     case LogIn(username, replyTo) =>
//       replyTo ! ActionPerformed(s"User $username logged in.")
//       manageState(LoggedIn(username)) // Transition to logged-in state

//     case LogOut(replyTo) =>
//       replyTo ! ActionPerformed("User logged out.")
//       manageState(LoggedOut) // Transition to logged-out state

//     case GetStatus(replyTo) =>
//       replyTo ! UserStatus(state) // Send current state (logged in or out)
//       Behaviors.same

//     case SendMessage(message) =>
//       state match {
//         case LoggedIn(username) =>
//           println(s"User $username received message: $message")
//         case LoggedOut =>
//           println("Message not sent because user is logged out.")
//       }
//       Behaviors.same
//   }
// }