package com.itforelead.workout.services

import cats.data.OptionT
import cats.effect.{Resource, Sync}
import com.itforelead.workout.domain.{ID, Payment}
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentFilter, PaymentWithMember, PaymentWithTotal}
import com.itforelead.workout.domain.custom.exception.{CreatePaymentDailyTypeError, MemberNotFound}
import com.itforelead.workout.domain.types.{MemberId, PaymentId, UserId}
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.PaymentSQL._
import cats.syntax.all._
import com.itforelead.workout.domain.PaymentType.{DAILY, MONTHLY}
import com.itforelead.workout.implicits.LocalDateTimeOps
import com.itforelead.workout.services.sql.PaymentSQL
import skunk._
import skunk.implicits.toIdOps

import java.time.LocalDateTime

trait Payments[F[_]] {
  def payments(userId: UserId): F[List[PaymentWithMember]]
  def getPaymentByMemberId(userId: UserId, memberId: MemberId): F[List[Payment]]
  def getPaymentsWithTotal(userId: UserId, filter: PaymentFilter, page: Int): F[PaymentWithTotal]
  def create(userId: UserId, payment: CreatePayment): F[Payment]
}

object Payments {

  def apply[F[_]: GenUUID: Sync](
    userSettings: UserSettings[F],
    members: Members[F]
  )(implicit session: Resource[F, Session[F]]): Payments[F] = new Payments[F] with SkunkHelper[F] {

    override def create(userId: UserId, payment: CreatePayment): F[Payment] = {
      def createPay(payment: CreatePayment, activeTime: LocalDateTime): F[Payment] =
        for {
          id           <- ID.make[F, PaymentId]
          userSettings <- userSettings.settings(userId)
          cost = payment.paymentType match {
            case MONTHLY => userSettings.monthlyPrice
            case DAILY   => userSettings.dailyPrice
          }
          now <- Sync[F].delay(LocalDateTime.now())
          payment <- prepQueryUnique(
            insert,
            Payment(
              id = id,
              userId = userId,
              memberId = payment.memberId,
              paymentType = payment.paymentType,
              cost = cost,
              createdAt = now
            )
          )
          _ <- members.updateActiveTime(payment.memberId, activeTime)
        } yield payment

      OptionT(members.findMemberById(payment.memberId))
        .cataF(
          MemberNotFound.raiseError[F, Payment],
          member =>
            Sync[F].delay(LocalDateTime.now()).flatMap { now =>
              if (now.isAfter(member.activeTime)) {
                payment.paymentType match {
                  case MONTHLY => createPay(payment, now.plusMonths(1).endOfDay)
                  case DAILY   => createPay(payment, now.endOfDay)
                }
              } else {
                payment.paymentType match {
                  case MONTHLY => createPay(payment, member.activeTime.plusMonths(1).endOfDay)
                  case DAILY   => CreatePaymentDailyTypeError.raiseError[F, Payment]
                }
              }
            }
        )
    }

    override def payments(userId: UserId): F[List[PaymentWithMember]] =
      prepQueryList(selectAll, userId)

    override def getPaymentsWithTotal(userId: UserId, filter: PaymentFilter, page: Int): F[PaymentWithTotal] =
      for {
        fr       <- selectPaymentWithTotal(userId, filter, page).pure[F]
        messages <- prepQueryList(fr.fragment.query(PaymentSQL.decPaymentWithMember), fr.argument)
        total    <- prepQueryUnique(total, userId)
      } yield PaymentWithTotal(messages, total)

    override def getPaymentByMemberId(userId: UserId, memberId: MemberId): F[List[Payment]] =
      prepQueryList(selectPaymentByMemberId, userId ~ memberId)

  }
}
