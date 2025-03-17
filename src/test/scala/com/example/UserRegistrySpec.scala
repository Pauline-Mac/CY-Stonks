package com.example

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import com.cystonks.actors.database.DatabaseActor
import com.cystonks.actors.user.UserRegistry
import com.cystonks.models.{User, Users}
import com.cystonks.routes.UserRoutes
import com.cystonks.models.json.UserJsonFormats._

class UserRegistrySpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  lazy val testKit = ActorTestKit()
  implicit def typedSystem = testKit.system
  override def createActorSystem() = testKit.system.toClassic

  val databaseActor: ActorRef[DatabaseActor.Command] = testKit.spawn(DatabaseActor())
  val userRegistry: ActorRef[UserRegistry.Command] = testKit.spawn(UserRegistry(databaseActor))
  lazy val routes = new UserRoutes(userRegistry).userRoutes

  "UserRegistry" should {
    "create a user (POST /users)" in {
      val user = User("a13d86f3-943c-4207-a4d6-9672d6ece0d8", "Liselott", "cy-stonks", Seq.empty, Seq.empty)
      val userEntity = Marshal(user).to[MessageEntity].futureValue

      val request = Post("/users").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should include("User a13d86f3-943c-4207-a4d6-9672d6ece0d8 saved.")
      }
    }

    "get a user (GET /users/{uuid})" in {
      val uuid = "a13d86f3-943c-4207-a4d6-9672d6ece0d8"
      val request = HttpRequest(uri = s"/users/$uuid")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should include("Liselott")
      }
    }

    "delete a user (DELETE /users/{uuid})" in {
      val uuid = "a13d86f3-943c-4207-a4d6-9672d6ece0d8"
      val request = Delete(uri = s"/users/$uuid")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should include("User a13d86f3-943c-4207-a4d6-9672d6ece0d8 deleted.")
      }
    }
  }
}