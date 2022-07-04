package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import cats.implicits._
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.custom.exception.PhoneInUse
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.{ID, Member, ValidationPhone}
import com.itforelead.workout.domain.types.{MemberId, UserId}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MemberSQL.{insertMember, selectByUserId}
import skunk.implicits._
import skunk.{Session, SqlState}

trait Members[F[_]] {
  def create(memberParam: CreateMember): F[Member]
  def findByUserId(userId: UserId): F[List[Member]]
  def findMemberByPhone(phone: Tel): F[Member]
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

      override def findByUserId(userId: UserId): F[List[Member]] =
        prepQueryList(selectByUserId, userId)

      override def findMemberByPhone(phone: Tel): F[Member] =
        prepQueryUnique(selectByUserId, phone)

    }

}
