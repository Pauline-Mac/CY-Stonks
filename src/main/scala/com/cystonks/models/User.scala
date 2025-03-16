package com.cystonks.models

import scala.collection.immutable

final case class User(uuid: String, username: String, password: String, wallets: Seq[String], financialInterests: Seq[String])
final case class Users(users: immutable.Seq[User])
