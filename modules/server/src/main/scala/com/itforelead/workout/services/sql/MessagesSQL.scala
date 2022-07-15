package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Message.MessageWithMember
import com.itforelead.workout.domain.types._
import com.itforelead.workout.domain.{DeliveryStatus, Message}
import com.itforelead.workout.services.sql.MemberSQL.memberId
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.codec.all.{int8, timestamp}
import skunk.implicits._

object MessagesSQL {
  val messageId: Codec[MessageId] = identity[MessageId]

  private val Columns = messageId ~ userId ~ memberId.opt ~ messageText ~ timestamp ~ deliveryStatus

  val encoder: Encoder[Message] =
    Columns.contramap(m => m.id ~ m.userId ~ m.memberId ~ m.text ~ m.sentDate ~ m.deliveryStatus)

  val decoder: Decoder[Message] =
    Columns.map { case id ~ userId ~ memberId ~ text ~ sentDate ~ deliveryStatus =>
      Message(id, userId, memberId, text, sentDate, deliveryStatus)
    }

  private val MessageColumns = decoder ~ MemberSQL.decoder.opt

  val decMessageWithMember: Decoder[MessageWithMember] =
    MessageColumns.map { case message ~ member =>
      MessageWithMember(message, member)
    }

  def selectMessagesWithPage(id: UserId, page: Int): AppliedFragment = {
    val filterByUserID: AppliedFragment =
      sql"""SELECT messages.*, members.* FROM messages
           LEFT JOIN members ON members.id = messages.member_id
           WHERE messages.user_id = $userId""".apply(id)
    filterByUserID.paginate(10, page)
  }

  val total: Query[UserId, Long] =
    sql"""SELECT count(*) FROM messages WHERE user_id = $userId""".query(int8)

  val insertMessage: Query[Message, Message] =
    sql"""INSERT INTO messages VALUES ($encoder) RETURNING *""".query(decoder)

  val select: Query[UserId, MessageWithMember] =
    sql"""SELECT messages.*, members.* FROM messages
          LEFT JOIN members ON members.id = messages.member_id
          WHERE messages.user_id = $userId
       """.query(decMessageWithMember)

  val changeStatusSql: Query[DeliveryStatus ~ MessageId, Message] =
    sql"""UPDATE messages SET delivery_status = $deliveryStatus WHERE id = $messageId RETURNING *""".query(decoder)

  val selectSentTodaySql: Query[Void, MemberId] =
    sql"""SELECT member_id FROM messages
         WHERE DATE(sent_date) = CURRENT_DATE AND member_id IS NOT NULL""".query(memberId)

}
