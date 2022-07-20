package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import com.itforelead.workout.domain.{DeliveryStatus, ID, Message}
import com.itforelead.workout.domain.Message.{CreateMessage, MessageWithMember, MessageWithTotal, MessagesFilter}
import com.itforelead.workout.domain.types.{MemberId, MessageId, UserId}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MessagesSQL._
import skunk._
import skunk.implicits.toIdOps
import cats.syntax.all._
import com.itforelead.workout.domain.custom.exception.MemberNotFound
import com.itforelead.workout.services.sql.MessagesSQL
import skunk.codec.all.int8

import java.time.LocalDateTime

trait Messages[F[_]] {
  def create(msg: CreateMessage): F[Message]
  def sentSMSTodayMemberIds: F[List[MemberId]]
  def get(userId: UserId): F[List[MessageWithMember]]
  def getMessagesWithTotal(userId: UserId, filter: MessagesFilter, page: Int): F[MessageWithTotal]
  def changeStatus(id: MessageId, status: DeliveryStatus): F[Message]
}

object Messages {
  def apply[F[_]: GenUUID: Sync](implicit
    session: Resource[F, Session[F]]
  ): Messages[F] =
    new Messages[F] with SkunkHelper[F] {

      override def create(msg: CreateMessage): F[Message] =
        (for {
          id  <- ID.make[F, MessageId]
          now <- Sync[F].delay(LocalDateTime.now())
          message <- prepQueryUnique(
            insertMessage,
            Message(id, msg.userId, msg.memberId, msg.text, now, msg.deliveryStatus)
          )
        } yield message).recoverWith { case SqlState.ForeignKeyViolation(_) =>
          MemberNotFound.raiseError[F, Message]
        }

      override def get(userId: UserId): F[List[MessageWithMember]] =
        prepQueryList(select, userId)

      override def sentSMSTodayMemberIds: F[List[MemberId]] =
        prepQueryList(selectSentTodaySql, Void)

      override def getMessagesWithTotal(userId: UserId, filter: MessagesFilter, page: Int): F[MessageWithTotal] =
        for {
          fr       <- selectMessagesWithTotal(userId, filter, page).pure[F]
          t        <- total(userId, filter).pure[F]
          messages <- prepQueryList(fr.fragment.query(MessagesSQL.decMessageWithMember), fr.argument)
          total    <- prepQueryUnique(t.fragment.query(int8), t.argument)
        } yield MessageWithTotal(messages, total)

      override def changeStatus(id: MessageId, status: DeliveryStatus): F[Message] =
        prepQueryUnique(changeStatusSql, status ~ id)
    }
}
