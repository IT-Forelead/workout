package com.itforelead.workout.security

import cats.Monad
import cats.syntax.all._
import com.itforelead.workout.effects.GenUUID
import dev.profunktor.auth.jwt._
import eu.timepit.refined.auto._
import pdi.jwt._
import com.itforelead.workout.implicits._
import com.itforelead.workout.types.{JwtAccessTokenKeyConfig, TokenExpiration}

trait Tokens[F[_]] {
  def create: F[JwtToken]
  def validateAndUpdate(claim: JwtClaim): F[Option[JwtToken]]
}

object Tokens {
  def make[F[_]: GenUUID: Monad](
    jwtExpire: JwtExpire[F],
    config: JwtAccessTokenKeyConfig,
    exp: TokenExpiration
  ): Tokens[F] =
    new Tokens[F] {
      private def encodeToken: JwtClaim => F[JwtToken] =
        jwtEncode[F](_, JwtSecretKey(config.secret), JwtAlgorithm.HS256)

      override def create: F[JwtToken] =
        for {
          uuid  <- GenUUID[F].make
          claim <- jwtExpire.expiresIn(JwtClaim(uuid.toJson), exp)
          token <- encodeToken(claim)
        } yield token

      override def validateAndUpdate(claim: JwtClaim): F[Option[JwtToken]] =
        jwtExpire
          .isExpired(claim)
          .asOptionT
          .semiflatMap { _ =>
            for {
              updated <- jwtExpire.expiresIn(claim, exp)
              token   <- encodeToken(updated)
            } yield token
          }
          .value
    }
}
