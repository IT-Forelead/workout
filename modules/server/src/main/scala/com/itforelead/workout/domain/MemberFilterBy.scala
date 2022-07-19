package com.itforelead.workout.domain

import cats.Show
import io.circe.{Decoder, Encoder}

sealed abstract class MemberFilterBy(val value: String)

object MemberFilterBy {
  case object FirstnameAZ extends MemberFilterBy("firstname-az")
  case object FirstnameZA extends MemberFilterBy("firstname-za")
  case object LastnameAZ  extends MemberFilterBy("firstname-az")
  case object LastnameZA  extends MemberFilterBy("firstname-za")
  case object ActiveTime  extends MemberFilterBy("active-time")

  val types: List[MemberFilterBy] = List(FirstnameAZ, FirstnameZA, LastnameAZ, LastnameZA, ActiveTime)

  def find(value: String): Option[MemberFilterBy] = types.find(_.value == value.toLowerCase)

  def unsafeFrom(value: String): MemberFilterBy =
    find(value).getOrElse(throw new IllegalArgumentException(s"value doesn't match [ $value ]"))

  implicit val encType: Encoder[MemberFilterBy] = Encoder.encodeString.contramap[MemberFilterBy](_.value)
  implicit val decType: Decoder[MemberFilterBy] = Decoder.decodeString.map(unsafeFrom)
  implicit val show: Show[MemberFilterBy]       = Show.show(_.value)
}
