package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import com.itforelead.workout.domain.User.{CreateClient, UserWithPassword}
import com.itforelead.workout.domain.custom.exception.{PhoneInUse, ValidationCodeExpired, ValidationCodeIncorrect}
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types.{GymName, UZS, UserId}
import com.itforelead.workout.domain.{ID, Role, User, UserSetting}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.redis.RedisClient
import com.itforelead.workout.services.sql.UserSQL.{insertUser, _}
import com.itforelead.workout.services.sql.UserSettingsSQL.insertSettings
import eu.timepit.refined.types.string.NonEmptyString
import skunk._
import skunk.implicits._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt

trait Users[F[_]] {
  def find(phoneNumber: Tel): F[Option[UserWithPassword]]
  def create(userParam: CreateClient, password: PasswordHash[SCrypt]): F[User]
  def getClients: F[List[User]]
}

object Users {

  def apply[F[_]: GenUUID: Sync](redis: RedisClient[F])(implicit
    session: Resource[F, Session[F]]
  ): Users[F] =
    new Users[F] with SkunkHelper[F] {

      def find(phoneNumber: Tel): F[Option[UserWithPassword]] =
        OptionT(prepOptQuery(selectUser, phoneNumber)).map { case user ~ p =>
          UserWithPassword(user, p)
        }.value

      def create(userParam: CreateClient, password: PasswordHash[SCrypt]): F[User] =
        for {
          id <- ID.make[F, UserId]
          code <- OptionT(redis.get(userParam.phone.value)).getOrElseF {
            ValidationCodeExpired(userParam.phone).raiseError[F, String]
          }
          user <- OptionT
            .whenF(code == userParam.code.value)(
              OptionT(find(userParam.phone))
                .semiflatMap(_ => PhoneInUse(userParam.phone).raiseError[F, User])
                .getOrElseF(prepQueryUnique(insertUser, id ~ userParam ~ password).flatMap { case (user, _) =>
                  prepQueryUnique(
                    insertSettings,
                    UserSetting(user.id, userParam.gymName, userParam.dailyPrice, userParam.monthlyPrice)
                  ).as(user)
                })
            )
            .getOrElseF {
              ValidationCodeIncorrect(userParam.code).raiseError[F, User]
            }
        } yield user

      def getClients: F[List[User]] =
        prepQueryList(selectClients, Void)
    }
}
