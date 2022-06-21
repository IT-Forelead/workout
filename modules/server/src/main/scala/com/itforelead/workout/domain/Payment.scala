package com.itforelead.workout.domain

import com.itforelead.workout.domain.custom.refinements.{FilePath, Tel}
import com.itforelead.workout.domain.types._
import derevo.cats._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import squants.Money
import io.circe.refined._
import eu.timepit.refined.cats._

import java.time.LocalDateTime

@derive(decoder, encoder, show)
case class Payment(
  id: PaymentId,
  userId: UserId,
  cost: Money,
  createdAt: LocalDateTime,
  expiredAt: LocalDateTime
)

object Payment {
  @derive(decoder, encoder, show)
  case class CreatePayment(
    userId: UserId,
    cost: Money,
    duration: Duration
  )

  @derive(decoder, encoder, show)
  case class PaymentWithUserInfo(
    payment: Payment,
    userName: UserName,
    userPhone: Tel,
    userPicture: FilePath,
    cost: Money,
    createdAt: LocalDateTime,
    expiredAt: LocalDateTime
  )
}