package com.cystonks.models.json

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import com.cystonks.models.{User, Users}
import com.cystonks.actors.user.UserRegistry

object UserJsonFormats extends DefaultJsonProtocol {
  implicit val userJsonFormat: RootJsonFormat[User] = jsonFormat5(User)
  implicit val usersJsonFormat: RootJsonFormat[Users] = jsonFormat1(Users)
  implicit val actionPerformedJsonFormat: RootJsonFormat[UserRegistry.ActionPerformed] = jsonFormat1(UserRegistry.ActionPerformed)
}
