package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import com.itforelead.workout.domain.{DeliveryStatus, ID, Message}
import com.itforelead.workout.domain.Message.{CreateMessage, MessageWithMember, MessageWithTotal}
import com.itforelead.workout.domain.types.{MessageId, UserId}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MessagesSQL._
import skunk.Session
import skunk.implicits.toIdOps
import cats.syntax.all._
import com.itforelead.workout.services.sql.MessagesSQL

import java.time.LocalDateTime

trait Messages[F[_]] {
  def create(msg: CreateMessage): F[Message]
  def get(userId: UserId): F[List[MessageWithMember]]
  def getMessagesWithTotal(userId: UserId, page: Int): F[MessageWithTotal]
  def changeStatus(id: MessageId, status: DeliveryStatus): F[Message]
}

object Messages {
  def apply[F[_]: GenUUID: Sync](implicit
    session: Resource[F, Session[F]]
  ): Messages[F] =
    new Messages[F] with SkunkHelper[F] {

      override def create(msg: CreateMessage): F[Message] =
        for {
          id  <- ID.make[F, MessageId]
          now <- Sync[F].delay(LocalDateTime.now())
          message <- prepQueryUnique(
            insertMessage,
            Message(id, msg.userId, msg.memberId, msg.text, now, msg.deliveryStatus)
          )
        } yield message

      override def get(userId: UserId): F[List[MessageWithMember]] =
        prepQueryList(select, userId)

      override def getMessagesWithTotal(userId: UserId, page: Int): F[MessageWithTotal] =
        for {
          fr       <- selectMessagesWithPage(userId, page).pure[F]
          messages <- prepQueryList(fr.fragment.query(MessagesSQL.decMessageWithMember), fr.argument)
          total    <- prepQueryUnique(total, userId)
        } yield MessageWithTotal(messages, total)

      override def changeStatus(id: MessageId, status: DeliveryStatus): F[Message] =
        prepQueryUnique(changeStatusSql, status ~ id)
    }
}
