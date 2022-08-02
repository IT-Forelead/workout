package com.itforelead.workout.domain

import cats.Show
import io.circe.{ Decoder, Encoder }

sealed abstract class MessageFilterBy(val value: String)

object MessageFilterBy {
  case object SendCode extends MessageFilterBy("send-code")
  case object ReminderSMS extends MessageFilterBy("reminder")

  val types: List[MessageFilterBy] = List(SendCode, ReminderSMS)

  def find(value: String): Option[MessageFilterBy] = types.find(_.value == value.toLowerCase)

  def unsafeFrom(value: String): MessageFilterBy =
    find(value).getOrElse(throw new IllegalArgumentException(s"value doesn't match [ $value ]"))

  implicit val encType: Encoder[MessageFilterBy] =
    Encoder.encodeString.contramap[MessageFilterBy](_.value)
  implicit val decType: Decoder[MessageFilterBy] = Decoder.decodeString.map(unsafeFrom)
  implicit val show: Show[MessageFilterBy] = Show.show(_.value)
}
