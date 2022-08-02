package com.itforelead.workout.domain

import com.itforelead.workout.domain.types.{ MemberId, MessageId, MessageText, UserId }
import derevo.cats.show
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive

import java.time.LocalDateTime

@derive(decoder, encoder, show)
case class Message(
    id: MessageId,
    userId: UserId,
    memberId: Option[MemberId],
    text: MessageText,
    sentDate: LocalDateTime,
    deliveryStatus: DeliveryStatus,
  )

object Message {
  @derive(decoder, encoder, show)
  case class CreateMessage(
      userId: UserId,
      memberId: Option[MemberId],
      text: MessageText,
      sentDate: LocalDateTime,
      deliveryStatus: DeliveryStatus,
    )

  @derive(decoder, encoder, show)
  case class MessageWithMember(
      message: Message,
      member: Option[Member],
    )

  @derive(decoder, encoder, show)
  case class MessageWithTotal(
      messages: List[MessageWithMember],
      total: Long,
    )

  @derive(decoder, encoder, show)
  case class MessagesFilter(
      typeBy: Option[MessageFilterBy] = None,
      filterDateFrom: Option[LocalDateTime] = None,
      filterDateTo: Option[LocalDateTime] = None,
    )
}
