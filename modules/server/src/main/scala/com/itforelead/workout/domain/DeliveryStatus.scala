package com.itforelead.workout.domain

import cats.Show
import io.circe.{ Decoder, Encoder }

sealed abstract class DeliveryStatus(val value: String)

object DeliveryStatus {
  case object SENT extends DeliveryStatus("sent")
  case object DELIVERED extends DeliveryStatus("delivered")
  case object FAILED extends DeliveryStatus("failed")
  case object UNKNOWN extends DeliveryStatus("unknown")

  val statuses: List[DeliveryStatus] = List(SENT, DELIVERED, FAILED, UNKNOWN)

  def find(value: String): Option[DeliveryStatus] =
    statuses.find(_.value == value.toLowerCase)

  def unsafeFrom(value: String): DeliveryStatus =
    find(value).getOrElse(throw new IllegalArgumentException(s"value doesn't match [ $value ]"))

  implicit val encStatus: Encoder[DeliveryStatus] =
    Encoder.encodeString.contramap[DeliveryStatus](_.value)
  implicit val decStatus: Decoder[DeliveryStatus] = Decoder.decodeString.map(unsafeFrom)
  implicit val show: Show[DeliveryStatus] = Show.show(_.value)
}
