package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect.std.Random
import cats.effect.{ Resource, Sync }
import com.itforelead.workout.domain.{ DeliveryStatus, ID, Message }
import com.itforelead.workout.domain.Message.{
  CreateMessage,
  MessageWithMember,
  MessageWithTotal,
  MessagesFilter,
}
import com.itforelead.workout.domain.types.{ MemberId, MessageId, MessageText, UserId }
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.MessagesSQL._
import skunk._
import skunk.implicits.toIdOps
import cats.syntax.all._
import com.itforelead.workout.domain.custom.exception.{
  AdminNotFound,
  MemberNotFound,
  UserNotFound,
}
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.services.redis.RedisClient
import com.itforelead.workout.services.sql.MessagesSQL
import eu.timepit.refined.types.string.NonEmptyString
import skunk.codec.all.int8

import scala.concurrent.duration.DurationInt
import java.time.LocalDateTime

trait Messages[F[_]] {
  def create(msg: CreateMessage): F[Message]
  def sentSMSTodayMemberIds: F[List[MemberId]]
  def sendValidationCode(userId: Option[UserId] = None, phone: Tel): F[Unit]
  def get(userId: UserId): F[List[MessageWithMember]]
  def getMessagesWithTotal(
      userId: UserId,
      filter: MessagesFilter,
      page: Int,
    ): F[MessageWithTotal]
  def changeStatus(id: MessageId, status: DeliveryStatus): F[Message]
}

object Messages {
  def apply[F[_]: GenUUID: Sync](
      redis: RedisClient[F],
      messageBroker: MessageBroker[F],
      users: Users[F],
    )(implicit
      session: Resource[F, Session[F]]
    ): Messages[F] =
    new Messages[F] with SkunkHelper[F] {
      override def create(msg: CreateMessage): F[Message] =
        (for {
          id <- ID.make[F, MessageId]
          now <- Sync[F].delay(LocalDateTime.now())
          message <- prepQueryUnique(
            insertMessage,
            Message(id, msg.userId, msg.memberId, msg.text, now, msg.deliveryStatus),
          )
        } yield message).recoverWith {
          case SqlState.ForeignKeyViolation(_) =>
            MemberNotFound.raiseError[F, Message]
        }

      override def get(userId: UserId): F[List[MessageWithMember]] =
        prepQueryList(select, userId)

      private def getUserId: F[Option[UserId]] =
        users.findAdmin.map(_.headOption.map(_.id))

      private def createAndSendMessage(
          userId: Option[UserId] = None,
          phone: Tel,
          code: Int,
          msgtext: MessageText,
          sentDate: LocalDateTime,
          status: DeliveryStatus,
        ): F[Unit] =
        OptionT
          .fromOption(userId)
          .orElseF(getUserId)
          .cataF(
            AdminNotFound.raiseError[F, Unit],
            adminId =>
              for {
                message <- create(CreateMessage(adminId, None, phone, msgtext, sentDate, status))
                _ <- redis.put(phone.value, code.toString, 3.minute)
                _ <- messageBroker.send(message.id, phone, msgtext.value.value)
              } yield (),
          )

      override def sendValidationCode(userId: Option[UserId] = None, phone: Tel): F[Unit] =
        Random.scalaUtilRandom[F].flatMap { implicit random =>
          for {
            now <- Sync[F].delay(LocalDateTime.now())
            validationCode <- random.betweenInt(1000, 9999)
            messageText = MessageText(
              NonEmptyString.unsafeFrom(s"Your Activation code is $validationCode")
            )
            _ <- createAndSendMessage(
              userId,
              phone,
              validationCode,
              messageText,
              now,
              DeliveryStatus.SENT,
            )
          } yield ()
        }

      override def sentSMSTodayMemberIds: F[List[MemberId]] =
        prepQueryList(selectSentTodaySql, Void)

      override def getMessagesWithTotal(
          userId: UserId,
          filter: MessagesFilter,
          page: Int,
        ): F[MessageWithTotal] =
        for {
          fr <- selectMessagesWithTotal(userId, filter, page).pure[F]
          t <- total(userId, filter).pure[F]
          messages <- prepQueryList(
            fr.fragment.query(MessagesSQL.decMessageWithMember),
            fr.argument,
          )
          total <- prepQueryUnique(t.fragment.query(int8), t.argument)
        } yield MessageWithTotal(messages, total)

      override def changeStatus(id: MessageId, status: DeliveryStatus): F[Message] =
        prepQueryUnique(changeStatusSql, status ~ id)
    }
}
