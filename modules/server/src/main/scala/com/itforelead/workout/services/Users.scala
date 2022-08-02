package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import com.itforelead.workout.domain.User.{
  CreateClient,
  UserActivate,
  UserFilter,
  UserWithPassword,
  UserWithTotal,
}
import com.itforelead.workout.domain.custom.exception.{
  PhoneInUse,
  ValidationCodeExpired,
  ValidationCodeIncorrect,
}
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.domain.{ ID, User, UserSetting }
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.redis.RedisClient
import com.itforelead.workout.services.sql.UserSQL
import com.itforelead.workout.services.sql.UserSQL._
import com.itforelead.workout.services.sql.UserSettingsSQL.insertSettings
import skunk._
import skunk.implicits._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt

trait Users[F[_]] {
  def find(phoneNumber: Tel): F[Option[UserWithPassword]]
  def findAdmin: F[List[User]]
  def create(userParam: CreateClient, password: PasswordHash[SCrypt]): F[User]
  def userActivate(userActivate: UserActivate): F[User]
  def getClients(filter: UserFilter, page: Int): F[UserWithTotal]
}

object Users {
  def apply[F[_]: GenUUID: Sync](
      redis: RedisClient[F]
    )(implicit
      session: Resource[F, Session[F]]
    ): Users[F] =
    new Users[F] with SkunkHelper[F] {
      def find(phoneNumber: Tel): F[Option[UserWithPassword]] =
        OptionT(prepOptQuery(selectUser, phoneNumber)).map {
          case user ~ p =>
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
                .getOrElseF(prepQueryUnique(insertUser, id ~ userParam ~ password).flatMap {
                  case (user, _) =>
                    prepQueryUnique(
                      insertSettings,
                      UserSetting(
                        user.id,
                        userParam.gymName,
                        userParam.dailyPrice,
                        userParam.monthlyPrice,
                      ),
                    ).as(user).flatTap(a => redis.del(a.phone.value))
                })
            )
            .getOrElseF {
              ValidationCodeIncorrect(userParam.code).raiseError[F, User]
            }
        } yield user

      def getClients(filter: UserFilter, page: Int): F[UserWithTotal] =
        for {
          fr <- selectClientsFilter(filter.typeBy, filter.sortBy, page).pure[F]
          clients <- prepQueryList(fr.fragment.query(UserSQL.decUserWithSetting), fr.argument)
          total <- prepQueryUnique(total, filter.sortBy)
        } yield UserWithTotal(clients, total)

      override def findAdmin: F[List[User]] =
        prepQueryList(selectAdmin, Void)

      override def userActivate(userActivate: UserActivate): F[User] =
        prepQueryUnique(changeActivateSql, userActivate.userId)
    }
}
