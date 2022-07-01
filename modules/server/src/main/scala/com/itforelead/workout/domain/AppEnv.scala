package com.itforelead.workout.domain

import cats.Show
import io.circe.{Decoder, Encoder}

sealed abstract class AppEnv(val value: String)

object AppEnv {
  case object TEST extends AppEnv("TEST")
  case object DEV  extends AppEnv("DEV")
  case object PROD extends AppEnv("PROD")

  val all: List[AppEnv] = List(TEST, DEV, PROD)

  def find(value: String): Option[AppEnv] =
    all.find(_.value.toLowerCase == value.toLowerCase)

  def unsafeFrom(value: String): AppEnv =
    find(value).getOrElse(throw new IllegalArgumentException(s"value doesn't match [ $value ]"))

  implicit val encStatus: Encoder[AppEnv] = Encoder.encodeString.contramap[AppEnv](_.value)
  implicit val decStatus: Decoder[AppEnv] = Decoder.decodeString.map(unsafeFrom)
  implicit val show: Show[AppEnv]         = Show.show(_.value)

}
