package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.{ Payment, PaymentType }
import com.itforelead.workout.domain.Payment.{ PaymentFilter, PaymentWithMember }
import com.itforelead.workout.domain.types.{ MemberId, PaymentId, UserId }
import com.itforelead.workout.services.sql.MemberSQL.memberId
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.codec.all.{ bool, int8, timestamp }
import skunk.implicits._

import java.time.LocalDateTime

object PaymentSQL {
  val paymentId: Codec[PaymentId] = identity[PaymentId]

  private val Columns =
    paymentId ~ UserSQL.userId ~ MemberSQL.memberId ~ paymentType ~ price ~ timestamp ~ bool

  val encoder: Encoder[Payment] =
    Columns.contramap { p =>
      p.id ~ p.userId ~ p.memberId ~ p.paymentType ~ p.cost ~ p.createdAt ~ false
    }

  val decoder: Decoder[Payment] =
    Columns.map {
      case i ~ ui ~ mi ~ pt ~ c ~ ca ~ _ =>
        Payment(i, ui, mi, pt, c, ca)
    }

  val decPaymentWithMember: Decoder[PaymentWithMember] =
    (decoder ~ MemberSQL.decoder).map {
      case payment ~ member =>
        PaymentWithMember(payment, member)
    }

  def typeFilter: Option[PaymentType] => Option[AppliedFragment] =
    _.map(sql""" payments.payment_type = $paymentType""")

  def startTimeFilter: Option[LocalDateTime] => Option[AppliedFragment] =
    _.map(sql"payments.created_at >= $timestamp")

  def endTimeFilter: Option[LocalDateTime] => Option[AppliedFragment] =
    _.map(sql"payments.created_at <= $timestamp")

  def selectPaymentWithTotal(
      id: UserId,
      params: PaymentFilter,
      page: Int,
    ): AppliedFragment = {
    val base: Fragment[UserId] = sql"""SELECT payments.*, members.* FROM payments
           LEFT JOIN members ON members.id = payments.member_id
           WHERE payments.user_id = $userId
          """

    val filters: List[AppliedFragment] =
      List(
        typeFilter(params.typeBy),
        startTimeFilter(params.filterDateFrom),
        endTimeFilter(params.filterDateTo),
      ).flatMap(_.toList)

    val filter: AppliedFragment =
      base(id).andOpt(filters) |+| sql" ORDER BY payments.created_at DESC".apply(Void)
    filter.paginate(10, page)
  }

  def total(id: UserId, params: PaymentFilter): AppliedFragment = {
    val base: Fragment[UserId] = sql"""SELECT count(*) FROM payments
           WHERE payments.user_id = $userId
          """

    val filters: List[AppliedFragment] =
      List(
        typeFilter(params.typeBy),
        startTimeFilter(params.filterDateFrom),
        endTimeFilter(params.filterDateTo),
      ).flatMap(_.toList)

    base(id).andOpt(filters)
  }

  val insert: Query[Payment, Payment] =
    sql"""INSERT INTO payments VALUES ($encoder) returning *""".query(decoder)

  val selectAll: Query[UserId, PaymentWithMember] =
    sql"""SELECT payments.*, members.*
        FROM payments
        INNER JOIN members ON members.id = payments.member_id
        WHERE payments.user_id = $userId AND payments.deleted = false
       """.query(decPaymentWithMember)

  val selectPaymentByMemberId: Query[UserId ~ MemberId, Payment] =
    sql"""SELECT * FROM payments
         WHERE user_id = $userId AND member_id = $memberId AND deleted = false
         ORDER BY created_at DESC""".query(decoder)
}
