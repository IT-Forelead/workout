package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Payment
import com.itforelead.workout.domain.Payment.PaymentWithUser
import com.itforelead.workout.domain.types.PaymentId
import com.itforelead.workout.services.sql.UserSQL.{userDecoder, userDecoderWithoutPass}
import skunk.codec.all.timestamp
import skunk.implicits._
import skunk._

object PaymentSql {
  val paymentId: Codec[PaymentId] = identity[PaymentId]

  private val Columns = paymentId ~ UserSQL.userId ~ price ~ timestamp ~ timestamp

  val encoder: Encoder[Payment] =
    Columns.contramap { p => p.id ~ p.userId ~ p.cost ~ p.createdAt ~ p.expiredAt }

  val paymentDecoder: Decoder[Payment] =
    Columns.map { case i ~ ui ~ c ~ ca ~ ea =>
      Payment(i, ui, c, ca, ea)
    }

  val decPaymentWithUser: Decoder[PaymentWithUser] =
    (paymentDecoder ~ userDecoderWithoutPass).map { case payment ~ user =>
      PaymentWithUser(payment, user)
    }

  val insert: Query[Payment, Payment] =
    sql"""INSERT INTO payments VALUES ($encoder) returning *""".query(paymentDecoder)

  val selectAll: Query[Void, PaymentWithUser] =
    sql"""SELECT payments.*, users.uuid, users.fullname, users.phone, users.birthday, users.user_picture, users.role 
        FROM payments
        INNER JOIN users ON users.uuid = payments.user_id
       """.query(decPaymentWithUser)

}
