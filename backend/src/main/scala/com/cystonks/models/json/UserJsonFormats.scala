package com.cystonks.models.json

import spray.json.DefaultJsonProtocol
import spray.json.RootJsonFormat
import com.cystonks.models.{User, Users,LoginRequest, AuthResponse, AuthenticationResponse}
import com.cystonks.actors.user.UserRegistry

object UserJsonFormats extends DefaultJsonProtocol {
  implicit val userJsonFormat: RootJsonFormat[User] = jsonFormat5(User)
  implicit val usersJsonFormat: RootJsonFormat[Users] = jsonFormat1(Users)
  implicit val actionPerformedJsonFormat: RootJsonFormat[UserRegistry.ActionPerformed] = jsonFormat1(UserRegistry.ActionPerformed)
  implicit val loginRequestJsonFormat: RootJsonFormat[LoginRequest] = jsonFormat2(LoginRequest)
  implicit val authResponseJsonFormat: RootJsonFormat[AuthResponse] = jsonFormat2(AuthResponse)
  implicit val authenticationResponseJsonFormat: RootJsonFormat[AuthenticationResponse] = jsonFormat2(AuthenticationResponse)
}
