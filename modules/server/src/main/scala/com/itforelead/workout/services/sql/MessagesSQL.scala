package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Message.{MessageWithMember, MessagesFilter}
import com.itforelead.workout.domain.MessageFilterBy.{ReminderSMS, SendCode}
import com.itforelead.workout.domain.types._
import com.itforelead.workout.domain.{DeliveryStatus, Message, MessageFilterBy}
import com.itforelead.workout.services.sql.MemberSQL.memberId
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.codec.all.{int8, timestamp}
import skunk.implicits._

import java.time.LocalDateTime

object MessagesSQL {
  val messageId: Codec[MessageId] = identity[MessageId]

  private val Columns = messageId ~ userId ~ memberId.opt ~ tel ~ messageText ~ timestamp ~ messageType ~ deliveryStatus

  val encoder: Encoder[Message] =
    Columns.contramap(m =>
      m.id ~ m.userId ~ m.memberId ~ m.phone ~ m.text ~ m.sentDate ~ m.messageType ~ m.deliveryStatus
    )

  val decoder: Decoder[Message] =
    Columns.map { case id ~ userId ~ memberId ~ phone ~ text ~ sentDate ~ messageType ~ deliveryStatus =>
      Message(id, userId, memberId, phone, text, sentDate, messageType, deliveryStatus)
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
           WHERE messages.user_id = $userId
           ORDER BY messages.sent_date DESC""".apply(id)
    filterByUserID.paginate(10, page)
  }

  def typeFilter: Option[MessageFilterBy] => Option[AppliedFragment] =
    _.map {
      case SendCode    => sql""" messages.member_id IS NULL """.apply(Void)
      case ReminderSMS => sql""" messages.member_id IS NOT NULL """.apply(Void)
    }

  def startTimeFilter: Option[LocalDateTime] => Option[AppliedFragment] =
    _.map(sql"messages.sent_date >= $timestamp")

  def endTimeFilter: Option[LocalDateTime] => Option[AppliedFragment] =
    _.map(sql"messages.sent_date <= $timestamp")

  def selectMessagesWithTotal(id: UserId, params: MessagesFilter, page: Int): AppliedFragment = {
    val base: Fragment[UserId] = sql"""SELECT messages.*, members.* FROM messages
          LEFT JOIN members ON members.id = messages.member_id
          WHERE messages.user_id = $userId
          """

    val filters: List[AppliedFragment] =
      List(
        typeFilter(params.typeBy),
        startTimeFilter(params.filterDateFrom),
        endTimeFilter(params.filterDateTo)
      ).flatMap(_.toList)

    val filter: AppliedFragment =
      base(id).andOpt(filters) |+| sql" ORDER BY messages.sent_date DESC".apply(Void)
    filter.paginate(10, page)
  }

  def total(id: UserId, params: MessagesFilter): AppliedFragment = {
    val base: Fragment[UserId] = sql"""SELECT count(*) FROM messages
          WHERE user_id = $userId
          """

    val filters: List[AppliedFragment] =
      List(
        typeFilter(params.typeBy),
        startTimeFilter(params.filterDateFrom),
        endTimeFilter(params.filterDateTo)
      ).flatMap(_.toList)

    base(id).andOpt(filters)
  }

  val insertMessage: Query[Message, Message] =
    sql"""INSERT INTO messages VALUES ($encoder) RETURNING *""".query(decoder)

  val select: Query[UserId, MessageWithMember] =
    sql"""SELECT messages.*, members.* FROM messages
          LEFT JOIN members ON members.id = messages.member_id
          WHERE messages.user_id = $userId
          ORDER BY messages.sent_date DESC
       """.query(decMessageWithMember)

  val changeStatusSql: Query[DeliveryStatus ~ MessageId, Message] =
    sql"""UPDATE messages SET delivery_status = $deliveryStatus WHERE id = $messageId RETURNING *""".query(decoder)

  val selectSentTodaySql: Query[Void, MemberId] =
    sql"""SELECT member_id FROM messages
         WHERE DATE(sent_date) = CURRENT_DATE AND member_id IS NOT NULL""".query(memberId)

}
