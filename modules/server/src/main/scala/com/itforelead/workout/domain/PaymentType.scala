package com.itforelead.workout.domain

import cats.Show
import io.circe.{ Decoder, Encoder }

sealed abstract class PaymentType(val value: String)

object PaymentType {
  case object MONTHLY extends PaymentType("monthly")
  case object DAILY extends PaymentType("daily")

  val paymentTypes: List[PaymentType] = List(MONTHLY, DAILY)

  def find(value: String): Option[PaymentType] = paymentTypes.find(_.value == value.toLowerCase)

  def unsafeFrom(value: String): PaymentType =
    find(value).getOrElse(throw new IllegalArgumentException(s"value doesn't match [ $value ]"))

  implicit val encType: Encoder[PaymentType] = Encoder.encodeString.contramap[PaymentType](_.value)

  implicit val decType: Decoder[PaymentType] = Decoder.decodeString.map(unsafeFrom)
  implicit val show: Show[PaymentType] = Show.show(_.value)
}
