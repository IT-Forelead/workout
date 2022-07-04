package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import com.itforelead.workout.domain.{DeliveryStatus, ID, Message}
import com.itforelead.workout.domain.Message.{CreateMessage, MessageWithMember}
import com.itforelead.workout.domain.types.{MessageId, UserId}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MessagesSQL._
import skunk.Session
import skunk.implicits.toIdOps
import cats.syntax.all._

import java.time.LocalDateTime

trait Messages[F[_]] {
  def create(msg: CreateMessage): F[Message]
  def get(userId: UserId): F[List[MessageWithMember]]
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

      override def changeStatus(id: MessageId, status: DeliveryStatus): F[Message] =
        prepQueryUnique(changeStatusSql, status ~ id)
    }
}
