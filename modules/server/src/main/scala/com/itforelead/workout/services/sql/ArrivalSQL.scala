package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Arrival
import com.itforelead.workout.domain.types._
import com.itforelead.workout.services.sql.MemberSQL.memberId
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.codec.all.{bool, timestamp}
import skunk.implicits._

object ArrivalSQL {
  val arrivalId: Codec[ArrivalId] = identity[ArrivalId]

  private val Columns = arrivalId ~ userId ~ memberId ~ timestamp ~ arrivalType ~ bool

  val encoder: Encoder[Arrival] =
    Columns.contramap(a => a.id ~ a.userId ~ a.memberId ~ a.createdAt ~ a.arrivalType ~ false)

  val decoder: Decoder[Arrival] =
    Columns.map { case id ~ userId ~ memberId ~ createdAt ~ arrivalType ~ _ =>
      Arrival(id, userId, memberId, createdAt, arrivalType)
    }

  val selectSql: Query[UserId, Arrival] =
    sql"""SELECT * FROM arrival_event WHERE user_id = $userId AND deleted = false""".query(decoder)

  val insertSql: Query[Arrival, Arrival] =
    sql"""INSERT INTO arrival_event VALUES ($encoder) RETURNING *""".query(decoder)

}
