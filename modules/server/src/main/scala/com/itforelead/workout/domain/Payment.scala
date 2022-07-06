package com.itforelead.workout.domain

import com.itforelead.workout.domain.types._
import derevo.cats._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.refined._
import eu.timepit.refined.cats._
import squants.Money

import java.time.LocalDateTime

@derive(decoder, encoder, show)
case class Payment(
  id: PaymentId,
  userId: UserId,
  memberId: MemberId,
  paymentType: PaymentType,
  cost: Money,
  createdAt: LocalDateTime
)

object Payment {
  @derive(decoder, encoder, show)
  case class CreatePayment(
    userId: UserId,
    memberId: MemberId,
    paymentType: PaymentType
  )

  @derive(decoder, encoder, show)
  case class PaymentWithMember(
    payment: Payment,
    member: Member
  )
}
