package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import com.itforelead.workout.domain.{ID, Member, Payment}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentWithMember}
import com.itforelead.workout.domain.custom.exception.UserIdIncorrect
import com.itforelead.workout.domain.types.{PaymentId, UserId}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.PaymentSQL.{insert, selectAll, selectExpiredMember}
import cats.syntax.all._
import com.itforelead.workout.domain.PaymentType.{DAILY, MONTHLY}
import skunk._

import java.time.LocalDateTime

trait Payments[F[_]] {
  def payments(userId: UserId): F[List[PaymentWithMember]]
  def create(payment: CreatePayment): F[Payment]
  def findExpireDateShort: F[List[Member]]
}

object Payments {

  def apply[F[_]: GenUUID: Sync](
    userSettings: UserSettings[F]
  )(implicit session: Resource[F, Session[F]]): Payments[F] = new Payments[F] with SkunkHelper[F] {

    override def create(payment: CreatePayment): F[Payment] = {
      (for {
        id           <- ID.make[F, PaymentId]
        userSettings <- userSettings.settings(payment.userId)
        now          <- Sync[F].delay(LocalDateTime.now())
        expiredAt = payment.paymentType match {
          case MONTHLY => now.plusMonths(1)
          case DAILY   => now.plusDays(1)
        }
        cost = payment.paymentType match {
          case MONTHLY => userSettings.monthlyPrice
          case DAILY   => userSettings.dailyPrice
        }
        payment <- prepQueryUnique(
          insert,
          Payment(
            id = id,
            userId = payment.userId,
            memberId = payment.memberId,
            paymentType = payment.paymentType,
            cost = cost,
            createdAt = now,
            expiredAt = expiredAt
          )
        )
      } yield payment).recoverWith { case SqlState.ForeignKeyViolation(_) =>
        UserIdIncorrect(payment.userId).raiseError[F, Payment]
      }
    }

    override def payments(userId: UserId): F[List[PaymentWithMember]] =
      prepQueryList(selectAll, userId)

    override def findExpireDateShort: F[List[Member]] =
      prepQueryList(selectExpiredMember, Void)
  }
}
