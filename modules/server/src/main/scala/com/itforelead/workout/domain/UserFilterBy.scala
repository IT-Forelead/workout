package com.itforelead.workout.domain

import cats.Show
import io.circe.{ Decoder, Encoder }

sealed abstract class UserFilterBy(val value: String)

object UserFilterBy {
  case object FirstnameAZ extends UserFilterBy("firstname-az")
  case object FirstnameZA extends UserFilterBy("firstname-za")
  case object LastnameAZ extends UserFilterBy("lastname-az")
  case object LastnameZA extends UserFilterBy("lastname-za")

  val types: List[UserFilterBy] = List(FirstnameAZ, FirstnameZA, LastnameAZ, LastnameZA)

  def find(value: String): Option[UserFilterBy] = types.find(_.value == value.toLowerCase)

  def unsafeFrom(value: String): UserFilterBy =
    find(value).getOrElse(throw new IllegalArgumentException(s"value doesn't match [ $value ]"))

  implicit val encType: Encoder[UserFilterBy] =
    Encoder.encodeString.contramap[UserFilterBy](_.value)
  implicit val decType: Decoder[UserFilterBy] = Decoder.decodeString.map(unsafeFrom)
  implicit val show: Show[UserFilterBy] = Show.show(_.value)
}
