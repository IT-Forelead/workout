package com.itforelead.workout.services

import cats.effect.Sync
import cats.implicits._
import com.itforelead.workout.config.SchedulerConfig
import com.itforelead.workout.domain.{ DeliveryStatus, Member, Message }
import com.itforelead.workout.domain.Message.CreateMessage
import com.itforelead.workout.domain.types.{ FirstName, MemberId, MessageText, UserId }
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.effects.Background
import eu.timepit.refined.types.string.NonEmptyString
import org.typelevel.log4cats.Logger

import java.time.{ LocalDateTime, LocalTime }
import scala.concurrent.duration.{ DurationInt, FiniteDuration }

trait NotificationMessage[F[_]] {
  def start: F[Unit]
}

object NotificationMessage {
  def make[F[_]: Sync: Logger: Background](
      members: Members[F],
      messages: Messages[F],
      messageBroker: MessageBroker[F],
      schedulerConfig: SchedulerConfig,
    ): NotificationMessage[F] =
    new NotificationMessage[F] {
      private val fixedTime: FiniteDuration = {
        val now = schedulerConfig.startTime.toSecondOfDay - LocalTime.now.toSecondOfDay
        if (now <= 0) 1.minute else now.seconds
      }

      override def start: F[Unit] =
        Logger[F].debug(
          Console.GREEN + s"NotificationMessage will start after $fixedTime" + Console.RESET
        ) >>
          Background[F].schedule(startSendExpireDate, fixedTime, schedulerConfig.period)

      private def startSendExpireDate: F[Unit] = {
        def text(firstName: FirstName): NonEmptyString =
          NonEmptyString.unsafeFrom(
            s"Assalomu alaykum ${firstName}. Sizning to'lov vaqtingiz tugashiga 3 kun qoldi."
          )
        for {
          membersList <- members.findActiveTimeShort
          sentTodayList <- messages.sentSMSTodayMemberIds
          _ <- membersList
            .filterNot { member =>
              sentTodayList.contains(member.id)
            }
            .traverse_ { member =>
              createMessage(member.userId, member.id, member.phone, text(member.firstname))
                .flatMap { message =>
                  send(member, text(member.firstname).value, message)
                }
            }
        } yield ()
      }

      private def createMessage(
          userId: UserId,
          memberId: MemberId,
          phone: Tel,
          text: NonEmptyString,
        ): F[Message] =
        Sync[F].delay(LocalDateTime.now()).flatMap { now =>
          messages.create(
            CreateMessage(userId, memberId.some, phone, MessageText(text), now, DeliveryStatus.SENT)
          )
        }

      private def send(
          member: Member,
          text: String,
          message: Message,
        ): F[Unit] =
        messages.changeStatus(message.id, status = DeliveryStatus.DELIVERED) >>
          messageBroker.send(message.id, member.phone, text)
    }
}
