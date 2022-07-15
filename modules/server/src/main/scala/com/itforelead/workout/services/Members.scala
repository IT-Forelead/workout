package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect.{Resource, Sync}
import cats.implicits._
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithTotal}
import com.itforelead.workout.domain.Message.CreateMessage
import com.itforelead.workout.domain.custom.exception.{PhoneInUse, ValidationCodeExpired, ValidationCodeIncorrect}
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel}
import com.itforelead.workout.domain.types.{MemberId, MessageText, UserId}
import com.itforelead.workout.domain.{DeliveryStatus, ID, Member}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MemberSQL._
import com.itforelead.workout.services.redis.RedisClient
import com.itforelead.workout.services.sql.MemberSQL
import eu.timepit.refined.types.string.NonEmptyString
import skunk.implicits._
import skunk.{Session, SqlState}

import java.time.LocalDateTime
import eu.timepit.refined.auto._

import scala.concurrent.duration.DurationInt

trait Members[F[_]] {
  def get(userId: UserId): F[List[Member]]
  def findByUserId(userId: UserId, page: Int): F[MemberWithTotal]
  def findMemberByPhone(phone: Tel): F[Option[Member]]
  def sendValidationCode(userId: UserId, phone: Tel): F[Unit]
  def validateAndCreate(userId: UserId, createMember: CreateMember, key: FileKey): F[Member]
  def findActiveTimeShort: F[List[Member]]
  def getWeekLeftOnAT(userId: UserId): F[List[Member]]
  def findMemberById(memberId: MemberId): F[Option[Member]]
  def updateActiveTime(memberId: MemberId, activeTime: LocalDateTime): F[Member]
}

object Members {
  def apply[F[_]: GenUUID: Sync](
    messageBroker: MessageBroker[F],
    messages: Messages[F],
    redis: RedisClient[F]
  )(implicit
    session: Resource[F, Session[F]]
  ): Members[F] =
    new Members[F] with SkunkHelper[F] {

      private def create(userId: UserId, memberParam: CreateMember, filePath: FileKey): F[Member] =
        (for {
          id     <- ID.make[F, MemberId]
          now    <- Sync[F].delay(LocalDateTime.now())
          member <- prepQueryUnique(insertMember, id ~ userId ~ memberParam ~ now ~ filePath)
        } yield member)
          .recoverWith { case SqlState.UniqueViolation(_) =>
            PhoneInUse(memberParam.phone).raiseError[F, Member]
          }

      override def get(userId: UserId): F[List[Member]] =
        prepQueryList(selectMembers, userId)

      override def findByUserId(userId: UserId, page: Int): F[MemberWithTotal] =
        for {
          fr     <- selectByUserId(userId, page).pure[F]
          member <- prepQueryList(fr.fragment.query(MemberSQL.decoder), fr.argument)
          total  <- prepQueryUnique(total, userId)
        } yield MemberWithTotal(member, total)

      override def findMemberByPhone(phone: Tel): F[Option[Member]] =
        prepOptQuery(selectByPhone, phone)

      override def sendValidationCode(userId: UserId, phone: Tel): F[Unit] =
        for {
          now            <- Sync[F].delay(LocalDateTime.now())
          validationCode <- Sync[F].delay(scala.util.Random.between(1000, 9999))
          messageText = MessageText(NonEmptyString.unsafeFrom(s"Your Activation code is $validationCode"))
          message <- messages.create(CreateMessage(userId, None, messageText, now, DeliveryStatus.SENT))
          _       <- redis.put(phone.value, validationCode.toString, 3.minute)
          _       <- messageBroker.send(message.id, phone, messageText.value)
        } yield ()

      override def validateAndCreate(userId: UserId, createMember: CreateMember, key: FileKey): F[Member] =
        for {
          code <- OptionT(redis.get(createMember.phone.value)).getOrElseF {
            ValidationCodeExpired(createMember.phone).raiseError[F, String]
          }
          member <- OptionT
            .whenF(code == createMember.code.value)(
              OptionT(findMemberByPhone(createMember.phone))
                .semiflatMap(_ => PhoneInUse(createMember.phone).raiseError[F, Member])
                .getOrElseF(create(userId, createMember, key))
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
