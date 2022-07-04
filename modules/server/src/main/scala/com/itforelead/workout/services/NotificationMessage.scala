package com.itforelead.workout.services

import cats.effect.Sync
import cats.implicits._
import com.itforelead.workout.config.SchedulerConfig
import com.itforelead.workout.domain.{DeliveryStatus, Member, Message}
import com.itforelead.workout.domain.Message.CreateMessage
import com.itforelead.workout.domain.types.{MemberId, Text, UserId}
import com.itforelead.workout.effects.Background
import eu.timepit.refined.types.string.NonEmptyString
import org.typelevel.log4cats.Logger

import java.time.{LocalDateTime, LocalTime}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait NotificationMessage[F[_]] {
  def start: F[Unit]
}

object NotificationMessage {
  def make[F[_]: Sync: Logger: Background](
    payments: Payments[F],
    messages: Messages[F],
    messageBroker: MessageBroker[F],
    schedulerConfig: SchedulerConfig
  ): NotificationMessage[F] =
    new NotificationMessage[F] {

      private val fixedTime: FiniteDuration = {
        val now = schedulerConfig.startTime.toSecondOfDay - LocalTime.now.toSecondOfDay
        if (now <= 0) 1.minute else now.seconds
      }

      override def start: F[Unit] =
        Logger[F].debug(Console.GREEN + s"NotificationMessage will start after $fixedTime" + Console.RESET) >>
          Background[F].schedule(startSendExpireDate, fixedTime, schedulerConfig.period)

      private def startSendExpireDate: F[Unit] =
        for {
          membersList <- payments.findExpireDateShort
          text = NonEmptyString.unsafeFrom(s"Sizning to'lov vaqtingizga 7 kundan oz muddat qoldi")
          _ <- membersList.traverse { member =>
            createMessage(member.userId, member.id, text).flatMap { message =>
              send(member, text.value, message)
            }
          }
        } yield ()

      private def createMessage(userId: UserId, memberId: MemberId, text: NonEmptyString): F[Message] = {
        Sync[F].delay(LocalDateTime.now()).flatMap { now =>
          messages.create(CreateMessage(userId, memberId, Text(text), now, DeliveryStatus.SENT))
        }
      }

      private def send(member: Member, text: String, message: Message): F[Unit] =
        messages.changeStatus(message.id, status = DeliveryStatus.DELIVERED) >>
          messageBroker.send(message.id, member.phone, text)

    }
}
