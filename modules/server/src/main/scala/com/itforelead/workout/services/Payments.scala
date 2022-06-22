package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import com.itforelead.workout.domain.{ID, Payment}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentWithUser}
import com.itforelead.workout.domain.custom.exception.UserIdIncorrect
import com.itforelead.workout.domain.types.PaymentId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.PaymentSql.{insert, selectAll}
import cats.syntax.all._
import skunk._

import java.time.LocalDateTime

trait Payments[F[_]] {
  def payments: F[List[PaymentWithUser]]
  def create(payment: CreatePayment): F[Payment]
}

object Payments {

  def apply[F[_]: GenUUID: Sync](implicit session: Resource[F, Session[F]]): Payments[F] = new Payments[F]
    with SkunkHelper[F] {

    override def create(payment: CreatePayment): F[Payment] =
      (for {
        id  <- ID.make[F, PaymentId]
        now <- Sync[F].delay(LocalDateTime.now())
        expiredAt = now.plusDays(payment.duration.value.value)
        payment <- prepQueryUnique(
          insert,
          Payment(
            id = id,
            userId = payment.userId,
            cost = payment.cost,
            createdAt = LocalDateTime.now(),
            expiredAt = expiredAt
          )
        )
      } yield payment).recoverWith { case SqlState.ForeignKeyViolation(_) =>
        UserIdIncorrect(payment.userId).raiseError[F, Payment]
      }

    override def payments: F[List[PaymentWithUser]] =
      prepQueryAll(selectAll)

  }
}
