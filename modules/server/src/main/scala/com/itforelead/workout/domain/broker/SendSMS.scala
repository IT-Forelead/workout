package com.itforelead.workout.domain.broker

import cats.data.NonEmptyList
import com.itforelead.workout.domain.broker.BrokerMessage.BrokerMessageWithoutMember
import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

@derive(decoder, encoder, show)
case class SendSMS(
  messages: NonEmptyList[BrokerMessage]
)

object SendSMS {
  @derive(decoder, encoder, show)
  case class SendSMSWithoutMember(
    messages: NonEmptyList[BrokerMessageWithoutMember]
  )
}
