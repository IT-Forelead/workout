package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel}
import com.itforelead.workout.domain.types._
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.codec.all.{bool, date,timestamp, int8}
import skunk.implicits._

import java.time.LocalDateTime

object MemberSQL {
  val memberId: Codec[MemberId] = identity[MemberId]

  private val Columns = memberId ~ userId ~ firstName ~ lastName ~ tel ~ date ~ timestamp ~ fileKey ~ bool

  val encoder: Encoder[MemberId ~ UserId ~ CreateMember ~ LocalDateTime ~ FileKey] =
    Columns.contramap { case i ~ userId ~ m ~ dt ~ key =>
      i ~ userId ~ m.firstname ~ m.lastname ~ m.phone ~ m.birthday ~ dt ~ key ~ false
    }

  val decoder: Decoder[Member] =
    Columns.map { case i ~ ui ~ fn ~ ln ~ p ~ b ~ at ~ fp ~ _ =>
      Member(i, ui, fn, ln, p, b, at, fp)
    }

<<<<<<<<< Temporary merge branch 1
  val memberDecoderWithTotal: Decoder[MemberWithTotal] =
    (memberId ~ userId ~ firstName ~ lastName ~ tel ~ date ~ timestamp ~ fileKey ~ bool ~ int4).map {
      case i ~ ui ~ fn ~ ln ~ p ~ b ~ at ~ fp ~ _ ~ t =>
        MemberWithTotal(Member(i, ui, fn, ln, p, b, at ~ fp), t)
    }

  def selectByUserId(id: UserId, page: Int): AppliedFragment = {
    val filterByUserID: AppliedFragment =
      sql"""SELECT * FROM members WHERE user_id = $userId AND deleted = false""".apply(id)
    filterByUserID.paginate(10, page)
  }

  val total: Query[UserId, Long] =
    sql"""SELECT count(*) FROM members WHERE user_id = $userId AND deleted = false""".query(int8)

    val selectByPhone: Query[Tel, Member] =
      sql"""SELECT * FROM members WHERE phone = $tel AND deleted = false""".query(decoder)

  val insertMember: Query[MemberId ~ UserId ~ CreateMember ~ LocalDateTime ~ FileKey, Member] =
    sql"""INSERT INTO members VALUES ($encoder) RETURNING *""".query(decoder)

  val changeActiveTimeSql: Query[LocalDateTime ~ MemberId, Member] =
    sql"""UPDATE members SET active_time = $timestamp WHERE id = $memberId RETURNING *""".query(decoder)

  val currentMemberActiveTimeSql: Query[MemberId, LocalDateTime] =
    sql"""SELECT active_time FROM members WHERE id = $memberId AND deleted = false""".query(timestamp)

  val selectExpiredMember: Query[Void, Member] =
    sql"""SELECT * FROM members
      WHERE active_time - INTERVAL '3 DAY' < NOW() AND
      NOW() < active_time""".query(decoder)

  val selectMemberByIdSql: Query[MemberId, Member] =
    sql"""SELECT * FROM members WHERE id = $memberId""".query(decoder)

}
