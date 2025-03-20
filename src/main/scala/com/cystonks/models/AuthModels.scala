package com.cystonks.models

// Modèle pour la réponse d'authentification interne
case class AuthenticationResponse(token: Option[String], user: Option[User])


case class LoginRequest(
                         username: String,
                         password: String
                       )


case class AuthResponse(
                         token: String,
                         user: User
                       )