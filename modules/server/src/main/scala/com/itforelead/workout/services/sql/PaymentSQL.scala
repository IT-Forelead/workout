package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Payment
import com.itforelead.workout.domain.Payment.PaymentWithMember
import com.itforelead.workout.domain.types.{PaymentId, UserId}
import com.itforelead.workout.services.sql.MemberSQL.memberDecoder
import com.itforelead.workout.services.sql.UserSQL.{userDecoderWithoutPass, userId}
import skunk.codec.all.{timestamp, uuid}
import skunk.implicits._
import skunk._

object PaymentSQL {
  val paymentId: Codec[PaymentId] = identity[PaymentId]

  private val Columns = paymentId ~ UserSQL.userId ~ MemberSQL.memberId ~ paymentType ~ price ~ timestamp ~ timestamp

  val encoder: Encoder[Payment] =
    Columns.contramap { p => p.id ~ p.userId ~ p.memberId ~ p.paymentType ~ p.cost ~ p.createdAt ~ p.expiredAt }

  val paymentDecoder: Decoder[Payment] =
    Columns.map { case i ~ ui ~ mi ~ pt ~ c ~ ca ~ ea =>
      Payment(i, ui, mi, pt, c, ca, ea)
    }

  val decPaymentWithMember: Decoder[PaymentWithMember] =
    (paymentDecoder ~ memberDecoder).map { case payment ~ member =>
      PaymentWithMember(payment, member)
    }

  val insert: Query[Payment, Payment] =
    sql"""INSERT INTO payments VALUES ($encoder) returning *""".query(paymentDecoder)

  val selectAll: Query[UserId, PaymentWithMember] =
    sql"""SELECT payments.*, members.*
        FROM payments
        INNER JOIN members ON members.id = payments.member_id
        WHERE payments.user_id = $userId
       """.query(decPaymentWithMember)

}