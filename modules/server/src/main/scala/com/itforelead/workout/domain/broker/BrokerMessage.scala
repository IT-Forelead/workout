package com.itforelead.workout.domain.broker

import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types.MessageId
import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.refined._
import eu.timepit.refined.cats._

@derive(decoder, encoder, show)
case class BrokerMessage(
  recipient: Tel,
  messageId: MessageId,
  text: String,
  sms: SMS
)

object BrokerMessage {
  @derive(decoder, encoder, show)
  case class BrokerMessageWithoutMember(
    recipient: Tel,
    text: String,
    sms: SMS
  )
}
