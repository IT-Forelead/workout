package com.itforelead.workout.services

import cats.effect.std.Random
import cats.effect.{Resource, Sync}
import com.itforelead.workout.domain.{DeliveryStatus, ID, Message}
import com.itforelead.workout.domain.Message.{CreateMessage, MessageWithMember, MessageWithTotal, MessagesFilter}
import com.itforelead.workout.domain.types.{MemberId, MessageId, MessageText, UserId}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MessagesSQL._
import skunk._
import skunk.implicits.toIdOps
import cats.syntax.all._
import com.itforelead.workout.domain.custom.exception.MemberNotFound
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.services.redis.RedisClient
import com.itforelead.workout.services.sql.MessagesSQL
import eu.timepit.refined.types.string.NonEmptyString
import skunk.codec.all.int8

import scala.concurrent.duration.DurationInt
import java.time.LocalDateTime
import java.util.UUID

trait Messages[F[_]] {
  def create(msg: CreateMessage): F[Message]
  def sentSMSTodayMemberIds: F[List[MemberId]]
  def sendValidationCode(
    userId: UserId = UserId(UUID.fromString("76c2c44c-8fbf-4184-9199-19303a042fa0")),
    phone: Tel
  ): F[Unit]
  def get(userId: UserId): F[List[MessageWithMember]]
  def getMessagesWithTotal(userId: UserId, filter: MessagesFilter, page: Int): F[MessageWithTotal]
  def changeStatus(id: MessageId, status: DeliveryStatus): F[Message]
}

object Messages {
  def apply[F[_]: GenUUID: Sync](redis: RedisClient[F], messageBroker: MessageBroker[F])(implicit
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

      override def sendValidationCode(userId: UserId, phone: Tel): F[Unit] = {
        Random.scalaUtilRandom[F].flatMap { implicit random =>
          for {
            now            <- Sync[F].delay(LocalDateTime.now())
            validationCode <- random.betweenInt(1000, 9999)
            messageText = MessageText(NonEmptyString.unsafeFrom(s"Your Activation code is $validationCode"))
            message <- create(CreateMessage(userId, None, messageText, now, DeliveryStatus.SENT))
            _       <- redis.put(phone.value, validationCode.toString, 3.minute)
            _       <- messageBroker.send(message.id, phone, messageText.value.value)
          } yield ()
        }
      }

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
