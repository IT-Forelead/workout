package com.itforelead.workout.domain

import com.itforelead.workout.domain.types.{MemberId, MessageId, Text, UserId}
import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.refined._
import eu.timepit.refined.cats._

import java.time.LocalDateTime

@derive(decoder, encoder, show)
case class Message(
  id: MessageId,
  userId: UserId,
  memberId: MemberId,
  text: Text,
  sentDate: LocalDateTime,
  deliveryStatus: DeliveryStatus
)

object Message {
  @derive(decoder, encoder, show)
  case class CreateMessage(
    userId: UserId,
    memberId: MemberId,
    text: Text,
    sentDate: LocalDateTime,
    deliveryStatus: DeliveryStatus
  )

  @derive(decoder, encoder, show)
  case class MessageWithMember(
    message: Message,
    member: Member
  )
}
