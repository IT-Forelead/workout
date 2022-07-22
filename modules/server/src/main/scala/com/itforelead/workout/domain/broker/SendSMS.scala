package com.itforelead.workout.domain.broker

import cats.data.NonEmptyList
import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

@derive(decoder, encoder, show)
case class SendSMS(
  messages: NonEmptyList[BrokerMessage]
)