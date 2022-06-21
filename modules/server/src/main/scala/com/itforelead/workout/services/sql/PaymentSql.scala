package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Payment
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentWithUserInfo}
import com.itforelead.workout.domain.types.PaymentId
import com.itforelead.workout.services.sql.PaymentSql.paymentId
import skunk.codec.all.timestamp
import skunk.implicits._
import skunk.{~, _}

object PaymentSql {
  val paymentId: Codec[PaymentId] = identity[PaymentId]

  private val Columns = paymentId ~ UserSQL.userId ~ price ~ timestamp ~ timestamp

  private val ColumnsPay = paymentId ~ UserSQL.userId ~ price ~ duration

  //  private val ColumnsPayment = paymentId ~

  val encoder: Encoder[PaymentId ~ CreatePayment] =
    ColumnsPay.contramap { case i ~ m =>
      i ~ m.userId ~ m.cost ~ m.duration
    }

  val decoder: Decoder[Payment] =
    Columns.map { case i ~ ui ~ c ~ ca ~ ea =>
      Payment(i, ui, c, ca, ea)
    }

  val decMatchWithUserName: Decoder[PaymentWithUserInfo] =
    ColumnsPayment.map { case mi ~ si ~ un ~ st ~ et =>
      MatchWithUserName(mi, si, un, st, et)
    }

  val insert: Query[PaymentId ~ CreatePayment, Payment] =
    sql"""INSERT INTO payments VALUES ($encoder) returning *""".query(decoder)

  val selectAll: Query[Void, MatchWithUserName] =
    sql"""SELECT m.uuid, m.stadium_id, u.name, m.start_time, m.end_time FROM matches AS m
         INNER JOIN users u on u.uuid = m.uuid
       """.query(decMatchWithUserName)

}
