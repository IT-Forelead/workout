package com.itforelead.workout.domain

import cats.Show
import io.circe.{Decoder, Encoder}

sealed abstract class ArrivalType(val value: String)

object ArrivalType {
  case object COMEIN extends ArrivalType("come_in")
  case object GOOUT  extends ArrivalType("go_out")

  val arrivalTypes: List[ArrivalType] = List(COMEIN, GOOUT)

  def find(value: String): Option[ArrivalType] = arrivalTypes.find(_.value == value.toLowerCase)

  def unsafeFrom(value: String): ArrivalType =
    find(value).getOrElse(throw new IllegalArgumentException(s"value doesn't match [ $value ]"))

  implicit val encType: Encoder[ArrivalType] = Encoder.encodeString.contramap[ArrivalType](_.value)
  implicit val decType: Decoder[ArrivalType] = Decoder.decodeString.map(unsafeFrom)
  implicit val show: Show[ArrivalType]       = Show.show(_.value)
}
