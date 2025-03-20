package com.cystonks.utils

import java.security.MessageDigest
import java.time.Instant
import scala.util.Try
import pdi.jwt.{JwtAlgorithm, JwtClaim}
import pdi.jwt.Jwt

object JwtUtils {
  private val secretKey = "ebc5ae0c1ad9464d59ded0d483281e70e46c557604735df277c7814ea28caa9f80e89f9f9f90b9e55802d2f0448d98f7b591a61d1041253aec9f9a70f1e8d376503fc6c6968d8de40d9ae142ad85533f39cae68c32350a08d6888b6a8fff11dc30e360d8c5c2bed986d8f036a8363bbb0692f42fd9a1937cd67dfa4c1a039112ee3bfdf18e2ecedd5a292d71112e50a29f3f702899cedf5f93fa7e5ea7bf66903990be81d24221dda3c0062e5d1bb580ea775f722c678b704e54ba312d0e4a3e05d64d31cf4f1df1793b0c976bce101e06939f325778ca5f8dfeb4e76230a8ee9fead0494b86fe98f1cd1903b41c8801b08c1738bd10fb580734a1e2047f9ee9"
  private val algorithm = JwtAlgorithm.HS256
  private val expirationTime = 86400

  def createToken(userUuid: String): String = {
    val claims = JwtClaim(
      subject = Some(userUuid),
      expiration = Some(Instant.now.plusSeconds(expirationTime).getEpochSecond),
      issuedAt = Some(Instant.now.getEpochSecond)
    )
    Jwt.encode(claims, secretKey, algorithm)
  }

  def validateToken(token: String): Try[JwtClaim] = {
    Jwt.decode(token, secretKey, Seq(algorithm))
  }

  def hashPassword(password: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(password.getBytes("UTF-8"))
    hash.map("%02x".format(_)).mkString
  }

  def verifyPassword(password: String, hashedPassword: String): Boolean = {
    hashPassword(password) == hashedPassword
  }
}