package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect.{Resource, Sync}
import cats.implicits._
import com.itforelead.workout.domain.PaymentType.{DAILY, MONTHLY}
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithTotal}
import com.itforelead.workout.domain.custom.exception.PhoneInUse
import com.itforelead.workout.domain.custom.refinements.FileKey
import com.itforelead.workout.domain.{ID, Member, PaymentType}
import com.itforelead.workout.implicits.LocalDateTimeOps
import com.itforelead.workout.domain.types.{MemberId, UserId}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MemberSQL._
import skunk.implicits._
import skunk.{Session, SqlState, Void, ~}

import java.time.LocalDateTime

trait Members[F[_]] {
  def create(memberParam: CreateMember, filePath: FileKey): F[Member]
  def findByUserId(userId: UserId, page: Int): F[List[MemberWithTotal]]
  def findActiveTimeShort: F[List[Member]]
  def updateActiveTime(memberId: MemberId, activeTime: LocalDateTime, paymentType: PaymentType): F[Member]
  def findMemberById(memberId: MemberId): F[Option[Member]]
  def updateActiveTime(memberId: MemberId, activeTime: LocalDateTime): F[Member]
}

object Members {
  def apply[F[_]: GenUUID: Sync](implicit
    session: Resource[F, Session[F]]
  ): Members[F] =
    new Members[F] with SkunkHelper[F] {

      def create(memberParam: CreateMember, filePath: FileKey): F[Member] = {
        (for {
          id     <- ID.make[F, MemberId]
          now    <- Sync[F].delay(LocalDateTime.now())
          member <- prepQueryUnique(insertMember, id ~ memberParam ~ now ~ filePath)
        } yield member)
          .recoverWith { case SqlState.UniqueViolation(_) =>
            PhoneInUse(memberParam.phone).raiseError[F, Member]
          }
      }

      override def findByUserId(userId: UserId, page: Int): F[List[MemberWithTotal]] = {
        val af = selectByUserId(userId, page)
        prepQueryList(af.fragment.query(memberDecoderWithTotal), af.argument)
      }

      override def findActiveTimeShort: F[List[Member]] =
        prepQueryList(selectExpiredMember, Void)

      override def findMemberById(memberId: MemberId): F[Option[Member]] =
        OptionT(prepOptQuery(selectMemberByIdSql, memberId)).value

      override def updateActiveTime(memberId: MemberId, activeTime: LocalDateTime): F[Member] =
        prepQueryUnique(changeActiveTimeSql, activeTime ~ memberId)


      override def findActiveTimeShort: F[List[Member]] =
        prepQueryList(selectExpiredMember, Void)

      override def updateActiveTime(
        memberId: MemberId,
        activeTime: LocalDateTime,
        paymentType: PaymentType
      ): F[Member] = {

        def activeTimeDirection(
          newAT: LocalDateTime,
          currentAT: LocalDateTime,
          paymentType: PaymentType
        ): LocalDateTime = {
          if (newAT.isAfter(currentAT)) {
            val updateCurrentAT = paymentType match {
              case MONTHLY => currentAT.plusMonths(1)
              case DAILY   => currentAT.plusDays(1)
            }
            updateCurrentAT
          } else {
            val updateCurrentAT = paymentType match {
              case MONTHLY => currentAT.plusMonths(1)
              case DAILY   => currentAT.plusDays(1)
            }
            updateCurrentAT
          }
        }

        for {
          currentActiveTime <- prepQueryUnique(currentMemberActiveTimeSql, memberId)
          member <- prepQueryUnique(
            changeActiveTimeSql,
            activeTimeDirection(activeTime, currentActiveTime, paymentType) ~ memberId
          )
        } yield member
      }

    }

}
