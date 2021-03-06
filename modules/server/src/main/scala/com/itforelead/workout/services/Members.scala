package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect.{ Resource, Sync }
import cats.implicits._
import com.itforelead.workout.domain.Member.{ CreateMember, MemberFilter, MemberWithTotal }
import com.itforelead.workout.domain.custom.exception.{
  PhoneInUse,
  ValidationCodeExpired,
  ValidationCodeIncorrect,
}
import com.itforelead.workout.domain.custom.refinements.{ FileKey, Tel }
import com.itforelead.workout.domain.types.{ MemberId, UserId }
import com.itforelead.workout.domain.{ ID, Member }
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MemberSQL._
import com.itforelead.workout.services.redis.RedisClient
import com.itforelead.workout.services.sql.MemberSQL
import skunk.implicits._
import skunk.{ Session, SqlState }

import java.time.LocalDateTime

trait Members[F[_]] {
  def get(userId: UserId): F[List[Member]]
  def membersWithTotal(
      userId: UserId,
      filter: MemberFilter,
      page: Int,
    ): F[MemberWithTotal]
  def findMemberByPhone(phone: Tel): F[Option[Member]]

  def validateAndCreate(
      userId: UserId,
      createMember: CreateMember,
      key: FileKey,
    ): F[Member]
  def findActiveTimeShort: F[List[Member]]
  def getWeekLeftOnAT(userId: UserId): F[List[Member]]
  def findMemberById(memberId: MemberId): F[Option[Member]]
  def updateActiveTime(memberId: MemberId, activeTime: LocalDateTime): F[Member]
}

object Members {
  def apply[F[_]: GenUUID: Sync](
      redis: RedisClient[F]
    )(implicit
      session: Resource[F, Session[F]]
    ): Members[F] =
    new Members[F] with SkunkHelper[F] {
      private def create(
          userId: UserId,
          memberParam: CreateMember,
          filePath: FileKey,
        ): F[Member] =
        (for {
          id <- ID.make[F, MemberId]
          now <- Sync[F].delay(LocalDateTime.now())
          member <- prepQueryUnique(insertMember, id ~ userId ~ memberParam ~ now ~ filePath)
        } yield member)
          .recoverWith {
            case SqlState.UniqueViolation(_) =>
              PhoneInUse(memberParam.phone).raiseError[F, Member]
          }

      override def get(userId: UserId): F[List[Member]] =
        prepQueryList(selectMembers, userId)

      override def membersWithTotal(
          userId: UserId,
          filter: MemberFilter,
          page: Int,
        ): F[MemberWithTotal] =
        for {
          fr <- selectMemberFilter(userId, filter.typeBy, page).pure[F]
          member <- prepQueryList(fr.fragment.query(MemberSQL.decoder), fr.argument)
          total <- prepQueryUnique(total, userId)
        } yield MemberWithTotal(member, total)

      override def findMemberByPhone(phone: Tel): F[Option[Member]] =
        prepOptQuery(selectByPhone, phone)

      override def validateAndCreate(
          userId: UserId,
          createMember: CreateMember,
          key: FileKey,
        ): F[Member] =
        for {
          code <- OptionT(redis.get(createMember.phone.value)).getOrElseF {
            ValidationCodeExpired(createMember.phone).raiseError[F, String]
          }
          member <- OptionT
            .whenF(code == createMember.code.value)(
              OptionT(findMemberByPhone(createMember.phone))
                .semiflatMap(_ => PhoneInUse(createMember.phone).raiseError[F, Member])
                .getOrElseF(create(userId, createMember, key))
                .flatTap(a => redis.del(a.phone.value))
            )
            .getOrElseF {
              ValidationCodeIncorrect(createMember.code).raiseError[F, Member]
            }
        } yield member

      override def findActiveTimeShort: F[List[Member]] =
        prepQueryAll(selectExpiredMembers)

      override def getWeekLeftOnAT(userId: UserId): F[List[Member]] =
        prepQueryList(selectWeekLeftOnAT, userId)

      override def findMemberById(memberId: MemberId): F[Option[Member]] =
        OptionT(prepOptQuery(selectMemberByIdSql, memberId)).value

      override def updateActiveTime(memberId: MemberId, activeTime: LocalDateTime): F[Member] =
        prepQueryUnique(changeActiveTimeSql, activeTime ~ memberId)
    }
}
