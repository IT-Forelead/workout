package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import cats.implicits._
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.custom.exception.PhoneInUse
import com.itforelead.workout.domain.{ID, Member}
import com.itforelead.workout.domain.types.MemberId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MemberSQL.insertMember
import skunk.implicits._
import skunk.{Session, SqlState}

trait Members[F[_]] {
  def create(memberParam: CreateMember): F[Member]
}

object Members {

  def apply[F[_]: GenUUID: Sync](implicit
    session: Resource[F, Session[F]]
  ): Members[F] =
    new Members[F] with SkunkHelper[F] {

      def create(memberParam: CreateMember): F[Member] = {
        ID.make[F, MemberId]
          .flatMap { id =>
            prepQueryUnique(insertMember, id ~ memberParam)
          }
          .recoverWith { case SqlState.UniqueViolation(_) =>
            PhoneInUse(memberParam.phone).raiseError[F, Member]
          }
      }
    }

}