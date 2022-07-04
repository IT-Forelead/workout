package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Payment
import com.itforelead.workout.domain.Payment.PaymentWithMember
import com.itforelead.workout.domain.types.{PaymentId, UserId}
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk.codec.all.{bool, timestamp}
import skunk.implicits._
import skunk._

object PaymentSQL {
  val paymentId: Codec[PaymentId] = identity[PaymentId]

  private val Columns =
    paymentId ~ UserSQL.userId ~ MemberSQL.memberId ~ paymentType ~ price ~ timestamp ~ timestamp ~ bool

  val encoder: Encoder[Payment] =
    Columns.contramap { p => p.id ~ p.userId ~ p.memberId ~ p.paymentType ~ p.cost ~ p.createdAt ~ p.expiredAt ~ false }

  val decoder: Decoder[Payment] =
    Columns.map { case i ~ ui ~ mi ~ pt ~ c ~ ca ~ ea ~ _ =>
      Payment(i, ui, mi, pt, c, ca, ea)
    }

  val decPaymentWithMember: Decoder[PaymentWithMember] =
    (decoder ~ MemberSQL.decoder).map { case payment ~ member =>
      PaymentWithMember(payment, member)
    }

  val insert: Query[Payment, Payment] =
    sql"""INSERT INTO payments VALUES ($encoder) returning *""".query(decoder)

  val selectAll: Query[UserId, PaymentWithMember] =
    sql"""SELECT payments.*, members.*
        FROM payments
        INNER JOIN members ON members.id = payments.member_id
        WHERE payments.user_id = $userId AND payments.deleted = false
       """.query(decPaymentWithMember)

}
