package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all._
import com.itforelead.workout.domain.User.{CreateUser, _}
import com.itforelead.workout.domain._
import com.itforelead.workout.domain.custom.exception.{InvalidPassword, PhoneInUse, UserNotFound}
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.security.Tokens
import com.itforelead.workout.services.redis.RedisClient
import com.itforelead.workout.types.TokenExpiration
import dev.profunktor.auth.jwt.JwtToken
import eu.timepit.refined.auto.autoUnwrap
import pdi.jwt.JwtClaim
import tsec.passwordhashers.jca.SCrypt

trait Auth[F[_]] {
  def newUser(userParam: CreateUser): F[JwtToken]
  def login(credentials: Credentials): F[JwtToken]
  def logout(token: JwtToken, phone: Tel): F[Unit]
  def prolongSession(claim: JwtClaim, token: JwtToken, user: User): F[Option[JwtToken]]
}

object Auth {
  def apply[F[_]: Sync](
    tokenExpiration: TokenExpiration,
    tokens: Tokens[F],
    users: Users[F],
    redis: RedisClient[F]
  ): Auth[F] =
    new Auth[F] {

      private val TokenExpiration = tokenExpiration.value

      override def newUser(userParam: CreateUser): F[JwtToken] =
        OptionT(users.find(userParam.phone)).cataF(
          for {
            hash <- SCrypt.hashpw[F](userParam.password)
            user <- users.create(userParam, hash)
            t    <- tokens.create
            _    <- redis.put(t.value, user, TokenExpiration)
            _    <- redis.put(user.phone, t.value, TokenExpiration)
          } yield t,
          _ => PhoneInUse(userParam.phone).raiseError[F, JwtToken]
        )

      override def login(credentials: Credentials): F[JwtToken] =
        users.find(credentials.phone).flatMap {
          case None =>
            UserNotFound(credentials.phone).raiseError[F, JwtToken]
          case Some(user) if !SCrypt.checkpwUnsafe(credentials.password, user.password) =>
            InvalidPassword(credentials.phone).raiseError[F, JwtToken]
          case Some(userWithPass) =>
            OptionT(redis.get(credentials.phone)).cataF(
              tokens.create.flatTap { t =>
                redis.put(t.value, userWithPass.user, TokenExpiration) >>
                  redis.put(credentials.phone, t.value, TokenExpiration)
              },
              token => JwtToken(token).pure[F]
            )
        }

      override def logout(token: JwtToken, phone: Tel): F[Unit] =
        redis.del(token.show, phone)

      override def prolongSession(claim: JwtClaim, token: JwtToken, user: User): F[Option[JwtToken]] =
        OptionT(tokens.validateAndUpdate(claim))
          .semiflatTap(newToken =>
            redis.del(token.value) >>
              redis.put(newToken.value, user, TokenExpiration) >>
              redis.put(user.phone, newToken.value, TokenExpiration)
          )
          .value
    }
}
