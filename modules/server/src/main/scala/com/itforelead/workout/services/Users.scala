package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import com.itforelead.workout.domain.User.{CreateClient, UserWithPassword}
import com.itforelead.workout.domain.custom.exception.PhoneInUse
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types.{GymName, UZS, UserId}
import com.itforelead.workout.domain.{ID, Role, User, UserSetting}
import com.itforelead.workout.effects.GenUUID
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

  def apply[F[_]: GenUUID: Sync](implicit
    session: Resource[F, Session[F]]
  ): Users[F] =
    new Users[F] with SkunkHelper[F] {

      def find(phoneNumber: Tel): F[Option[UserWithPassword]] =
        OptionT(prepOptQuery(selectUser, phoneNumber)).map { case user ~ p =>
          UserWithPassword(user, p)
        }.value

      def create(userParam: CreateClient, password: PasswordHash[SCrypt]): F[User] = {
        (for {
          id   <- ID.make[F, UserId]
          user <- prepQueryUnique(insertUser, id ~ userParam ~ password).map(_._1)
          _ <- prepQueryUnique(
            insertSettings,
            UserSetting(user.id, GymName(NonEmptyString.unsafeFrom("Demo GYM")), UZS(1000), UZS(1000))
          )
        } yield user)
          .recoverWith { case SqlState.UniqueViolation(_) =>
            PhoneInUse(userParam.phone).raiseError[F, User]
          }

      }

      def getClients: F[List[User]] =
        prepQueryList(selectClients, Void)
    }
}
