package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import com.itforelead.workout.domain.{ID, Payment}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentWithMember}
import com.itforelead.workout.domain.custom.exception.UserIdIncorrect
import com.itforelead.workout.domain.types.{PaymentId, UserId}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.PaymentSQL.{insert, selectAll}
import cats.syntax.all._
import skunk._

import java.time.LocalDateTime

trait Payments[F[_]] {
  def payments(userId: UserId): F[List[PaymentWithMember]]
  def create(payment: CreatePayment): F[Payment]
}

object Payments {

  def apply[F[_]: GenUUID: Sync](implicit session: Resource[F, Session[F]]): Payments[F] = new Payments[F]
    with SkunkHelper[F] {

    override def create(payment: CreatePayment): F[Payment] = {
      (for {
        id  <- ID.make[F, PaymentId]
        now <- Sync[F].delay(LocalDateTime.now())
        payment <- prepQueryUnique(
          insert,
          Payment(
            id = id,
            userId = payment.userId,
            memberId = payment.memberId,
            paymentType = payment.paymentType,
            cost = payment.cost,
            createdAt = LocalDateTime.now(),
            expiredAt = LocalDateTime.now()
          )
        )
      } yield payment).recoverWith { case SqlState.ForeignKeyViolation(_) =>
        UserIdIncorrect(payment.userId).raiseError[F, Payment]
      }
    }

    override def payments(userId: UserId): F[List[PaymentWithMember]] =
      prepQueryList(selectAll, userId)

  }
}
