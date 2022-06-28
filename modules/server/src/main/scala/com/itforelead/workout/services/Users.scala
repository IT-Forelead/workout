package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import com.itforelead.workout.domain.Member.Validation
import com.itforelead.workout.domain.User.{CreateUser, UserWithPassword}
import com.itforelead.workout.domain.custom.refinements.{Tel, ValidationCode}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.domain.{ID, Message, User}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.implicits.CirceDecoderOps
import com.itforelead.workout.services.redis.RedisClient
import com.itforelead.workout.services.sql.UserSQL._
import eu.timepit.refined.types.string.NonEmptyString
import skunk._
import skunk.implicits._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt

import scala.concurrent.duration.DurationInt

trait Users[F[_]] {
  def find(phoneNumber: Tel): F[Option[UserWithPassword]]
  def create(userParam: CreateUser, password: PasswordHash[SCrypt]): F[User]
  def sendValidationCode(phone: Tel): F[Unit]
  def validatePhone(validation: Validation): F[Boolean]
}

object Users {

  def apply[F[_]: GenUUID: Sync](messageBroker: MessageBroker[F], redis: RedisClient[F])(implicit
    session: Resource[F, Session[F]]
  ): Users[F] =
    new Users[F] with SkunkHelper[F] {

      def find(phoneNumber: Tel): F[Option[UserWithPassword]] =
        OptionT(prepOptQuery(selectUser, phoneNumber)).map { case user ~ p =>
          UserWithPassword(user, p)
        }.value

      def create(userParam: CreateUser, password: PasswordHash[SCrypt]): F[User] = {
        ID.make[F, UserId]
          .flatMap { id =>
            prepQueryUnique(insertUser, id ~ userParam ~ password).map(_._1)
          }
      }


    }

}
