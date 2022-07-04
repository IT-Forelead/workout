package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import cats.implicits._
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithTotal}
import com.itforelead.workout.domain.custom.exception.PhoneInUse
import com.itforelead.workout.domain.custom.refinements.FileKey
import com.itforelead.workout.domain.{ID, Member}
import com.itforelead.workout.domain.types.{MemberId, UserId}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MemberSQL.{insertMember, memberDecoder, selectByUserId, total}
import com.itforelead.workout.services.sql.MemberSQL.{insertMember, selectByPhone, selectByUserId}
import skunk.implicits._
import skunk.{Session, SqlState}

trait Members[F[_]] {
  def create(memberParam: CreateMember, filePath: FileKey): F[Member]
  def findByUserId(userId: UserId, page: Int): F[MemberWithTotal]
  def findMemberByPhone(phone: Tel): F[Option[Member]]
}

object Members {

  def apply[F[_]: GenUUID: Sync](implicit
    session: Resource[F, Session[F]]
  ): Members[F] =
    new Members[F] with SkunkHelper[F] {

      def create(memberParam: CreateMember, filePath: FileKey): F[Member] = {
        ID.make[F, MemberId]
          .flatMap { id =>
            prepQueryUnique(insertMember, id ~ memberParam ~ filePath)
          }
          .recoverWith { case SqlState.UniqueViolation(_) =>
            PhoneInUse(memberParam.phone).raiseError[F, Member]
          }
      }

      override def findByUserId(userId: UserId, page: Int): F[MemberWithTotal] = {
        for {
          fr     <- selectByUserId(userId, page).pure[F]
          member <- prepQueryList(fr.fragment.query(memberDecoder), fr.argument)
          total  <- prepQueryUnique(total, userId)
        } yield (MemberWithTotal(member, total))

      }

      override def findMemberByPhone(phone: Tel): F[Option[Member]] =
        prepOptQuery(selectByPhone, phone)

    }

}
