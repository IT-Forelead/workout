package com.itforelead.workout.services

import cats.effect.Sync
import cats.syntax.all._
import com.itforelead.workout.domain.User.CreateClient
import dev.profunktor.auth.jwt.JwtToken
import eu.timepit.refined.auto.autoUnwrap
import tsec.passwordhashers.jca.SCrypt
import com.itforelead.workout.domain._
import com.itforelead.workout.domain.User._
import com.itforelead.workout.domain.custom.exception.{InvalidPassword, PhoneInUse, UserNotActivated, UserNotFound}
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.security.Tokens
import com.itforelead.workout.services.redis.RedisClient
import com.itforelead.workout.types.TokenExpiration

trait Auth[F[_]] {
  def newUser(userParam: CreateClient): F[JwtToken]
  def login(credentials: Credentials): F[JwtToken]
  def logout(token: JwtToken, phone: Tel): F[Unit]
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

      override def newUser(userParam: CreateClient): F[JwtToken] =
        users.find(userParam.phone).flatMap {
          case Some(_) =>
            PhoneInUse(userParam.phone).raiseError[F, JwtToken]
          case None =>
            for {
              hash <- SCrypt.hashpw[F](userParam.password)
              user <- users.create(userParam, hash)
              t    <- tokens.create
              _    <- redis.put(t.value, user, TokenExpiration)
              _    <- redis.put(user.phone, t.value, TokenExpiration)
            } yield t
        }

      def login(credentials: Credentials): F[JwtToken] =
        users.find(credentials.phone).flatMap {
          case None =>
            UserNotFound(credentials.phone).raiseError[F, JwtToken]
          case Some(userWithPass) if !userWithPass.user.activate =>
            UserNotActivated.raiseError[F, JwtToken]
          case Some(user) if !SCrypt.checkpwUnsafe(credentials.password, user.password) =>
            InvalidPassword(credentials.phone).raiseError[F, JwtToken]
          case Some(userWithPass) =>
            redis.get(credentials.phone).flatMap {
              case Some(t) => JwtToken(t).pure[F]
              case None =>
                tokens.create.flatTap { t =>
                  redis.put(t.value, userWithPass.user, TokenExpiration) *>
                    redis.put(credentials.phone, t.value, TokenExpiration)
                }
            }
        }

      def logout(token: JwtToken, phone: Tel): F[Unit] =
        redis.del(token.show, phone)

    }
}
