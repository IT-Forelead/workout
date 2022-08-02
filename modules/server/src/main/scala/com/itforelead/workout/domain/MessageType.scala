package com.itforelead.workout.domain

import cats.Show
import io.circe.{Decoder, Encoder}

sealed abstract class MessageType(val value: String)

object MessageType {
  case object REMINDER   extends MessageType("reminder")
  case object SENTCODE   extends MessageType("sent_code")
  case object ACTIVATION extends MessageType("activation")

  val types: List[MessageType] = List(REMINDER, SENTCODE, ACTIVATION)

  def find(value: String): Option[MessageType] = types.find(_.value == value.toLowerCase)

  def unsafeFrom(value: String): MessageType =
    find(value).getOrElse(throw new IllegalArgumentException(s"value doesn't match [ $value ]"))

  implicit val encType: Encoder[MessageType] = Encoder.encodeString.contramap[MessageType](_.value)
  implicit val decType: Decoder[MessageType] = Decoder.decodeString.map(unsafeFrom)
  implicit val show: Show[MessageType]       = Show.show(_.value)
}
