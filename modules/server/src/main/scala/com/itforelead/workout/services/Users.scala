package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import com.itforelead.workout.domain.User.{CreateUser, UserWithPassword}
import com.itforelead.workout.domain.custom.exception.PhoneInUse
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.domain.{ID, User}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.UserSQL._
import skunk._
import skunk.implicits._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt

trait Users[F[_]] {
  def find(phoneNumber: Tel): F[Option[UserWithPassword]]
  def create(userParam: CreateUser, password: PasswordHash[SCrypt]): F[User]
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

      def create(userParam: CreateUser, password: PasswordHash[SCrypt]): F[User] = {
        ID.make[F, UserId]
          .flatMap { id =>
            prepQueryUnique(insertUser, id ~ userParam ~ password).map(_._1)
          }
          .recoverWith { case SqlState.UniqueViolation(_) => PhoneInUse(userParam.phone).raiseError[F, User] }
      }
    }
}
