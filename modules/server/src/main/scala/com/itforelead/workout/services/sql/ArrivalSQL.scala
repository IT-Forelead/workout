package com.itforelead.workout.services.sql

import cats.implicits.catsSyntaxOptionId
import com.itforelead.workout.domain.Arrival
import com.itforelead.workout.domain.Arrival.{ArrivalFilter, ArrivalWithMember}
import com.itforelead.workout.domain.types._
import com.itforelead.workout.services.sql.MemberSQL.memberId
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.codec.all.{bool, int8, timestamp}
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

  val decArrivalWithMember: Decoder[ArrivalWithMember] =
    (decoder ~ MemberSQL.decoder).map { case arrival ~ member =>
      ArrivalWithMember(arrival, member)
    }

  def selectArrivalWithTotal(id: UserId, params: ArrivalFilter, page: Int): AppliedFragment = {
    val base: Fragment[UserId] = sql"""SELECT arrival_event.*, members.* FROM arrival_event
          INNER JOIN members ON members.id = arrival_event.member_id
          WHERE arrival_event.user_id = $userId AND arrival_event.deleted = false
          """

    val filters: List[AppliedFragment] =
      List(
        arrivalTypeFilter(params.typeBy),
        arrivalStartTimeFilter(params.filterDateFrom),
        arrivalEndTimeFilter(params.filterDateTo)
      ).flatMap(_.toList)

    val filterByUserID: AppliedFragment =
      base(id).andOpt(filters) |+| sql" ORDER BY arrival_event.created_at DESC".apply(Void)
    filterByUserID.paginate(10, page)
  }

  val total: Query[UserId, Long] =
    sql"""SELECT count(*) FROM arrival_event WHERE user_id = $userId AND deleted = false""".query(int8)

  val selectSql: Query[UserId, ArrivalWithMember] =
    sql"""SELECT arrival_event.*, members.* FROM arrival_event
          INNER JOIN members ON members.id = arrival_event.member_id
         WHERE arrival_event.user_id = $userId AND arrival_event.deleted = false
         ORDER BY arrival_event.created_at DESC""".query(decArrivalWithMember)

  val insertSql: Query[Arrival, Arrival] =
    sql"""INSERT INTO arrival_event VALUES ($encoder) RETURNING *""".query(decoder)

  val selectArrivalByMemberId: Query[UserId ~ MemberId, Arrival] =
    sql"""SELECT * FROM arrival_event
         WHERE user_id = $userId AND member_id = $memberId AND deleted = false
         ORDER BY created_at DESC""".query(decoder)

}
