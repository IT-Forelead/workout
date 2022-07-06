package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithTotal}
import com.itforelead.workout.domain.custom.refinements.FileKey
import com.itforelead.workout.domain.types._
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.codec.all.{bool, date, timestamp, int4}
import skunk.implicits._

import java.time.LocalDateTime

object MemberSQL {
  val memberId: Codec[MemberId] = identity[MemberId]

  private val Columns = memberId ~ userId ~ firstName ~ lastName ~ tel ~ date ~ timestamp ~ fileKey ~ bool

  val encoder: Encoder[MemberId ~ CreateMember ~ LocalDateTime ~ FileKey] =
    Columns.contramap { case i ~ u ~ dt ~ key =>
      i ~ u.userId ~ u.firstname ~ u.lastname ~ u.phone ~ u.birthday ~ dt ~ key ~ false
    }

  val decoder: Decoder[Member] =
    Columns.map { case i ~ ui ~ fn ~ ln ~ p ~ b ~ at ~ fp ~ _ =>
      Member(i, ui, fn, ln, p, b, at, fp)
    }

  val selectByUserId: Query[UserId, Member] =
    sql"""SELECT * FROM members WHERE user_id = $userId AND deleted = false""".query(decoder)

  val memberDecoderWithTotal: Decoder[MemberWithTotal] =
    (memberId ~ userId ~ firstName ~ lastName ~ tel ~ date ~ timestamp ~ fileKey ~ bool ~ int4).map {
      case i ~ ui ~ fn ~ ln ~ p ~ b ~ at ~ fp ~ _ ~ t =>
        MemberWithTotal(Member(i, ui, fn, ln, p, b, at, fp), t)
    }

  def selectByUserId(id: UserId, page: Int): AppliedFragment = {
    val filterByUserID: AppliedFragment =
      sql"""SELECT m.*, COUNT(m.*) FROM members m WHERE m.user_id = $userId AND m.deleted = false""".apply(id)
    filterByUserID.paginate(10, page)
  }

  val insertMember: Query[MemberId ~ CreateMember ~ LocalDateTime ~ FileKey, Member] =
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
