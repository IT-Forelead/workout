package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import com.itforelead.workout.domain.{ID, Payment}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentWithUserInfo}
import com.itforelead.workout.domain.custom.exception.UserIdIncorrect
import com.itforelead.workout.domain.types.PaymentId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.PaymentSql.{insert, paymentId, selectAll}
import cats.syntax.all._
import eu.timepit.refined.types.time.Day
import skunk._
import skunk.implicits._

import java.time.LocalDateTime

trait Payments[F[_]] {
  def payments: F[List[PaymentWithUserInfo]]
  def create(payment: CreatePayment): F[Payment]
}

object Payments {

  def apply[F[_]: GenUUID: Sync](implicit session: Resource[F, Session[F]]): Payments[F] = new Payments[F]
    with SkunkHelper[F] {

    override def create(payment: CreatePayment): F[Payment] =
      (for {
        id  <- ID.make[F, PaymentId]
        now <- Sync[F].delay(LocalDateTime.now())
        expiredAt = now.plusDays(payment.duration.value)
        payment <- prepQueryUnique(insert, id ~ payment.userId ~ payment.cost ~ LocalDateTime.now() ~ expiredAt)
      } yield payment).recoverWith {
        case SqlState.ForeignKeyViolation(_) =>
          UserIdIncorrect(payment.userId).raiseError[F, Payment]
      }

    override def payments: F[List[PaymentWithUserInfo]] =
      prepQueryAll(selectAll)

  }
}
