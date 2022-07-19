package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.{Member, MemberFilterBy}
import com.itforelead.workout.domain.Member.{CreateMember, MemberFilter}
import com.itforelead.workout.domain.MemberFilterBy._
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel}
import com.itforelead.workout.domain.types._
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.codec.all.{bool, date, int8, timestamp}
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

  def selectMemberFilter(id: UserId, filter: Option[MemberFilterBy], page: Int): AppliedFragment = {
    val filterBy = filter
      .map {
        case FirstnameAZ => "ORDER BY firstname ASC"
        case FirstnameZA => "ORDER BY firstname DESC"
        case LastnameAZ  => "ORDER BY lastname ASC"
        case LastnameZA  => "ORDER BY lastname DESC"
        case ActiveTime  => "ORDER BY active_time DESC"
      }
      .getOrElse("")

    val res: AppliedFragment =
      sql"""SELECT * FROM members WHERE user_id = $userId AND deleted = false #$filterBy""".apply(id)
    res.paginate(10, page)
  }

  val total: Query[UserId, Long] =
    sql"""SELECT count(*) FROM members WHERE user_id = $userId AND deleted = false""".query(int8)

  val selectByPhone: Query[Tel, Member] =
    sql"""SELECT * FROM members WHERE phone = $tel AND deleted = false""".query(decoder)

  val selectMembers: Query[UserId, Member] =
    sql"""SELECT * FROM members WHERE user_id = $userId AND deleted = false""".query(decoder)

  val insertMember: Query[MemberId ~ UserId ~ CreateMember ~ LocalDateTime ~ FileKey, Member] =
    sql"""INSERT INTO members VALUES ($encoder) RETURNING *""".query(decoder)

  val changeActiveTimeSql: Query[LocalDateTime ~ MemberId, Member] =
    sql"""UPDATE members SET active_time = $timestamp WHERE id = $memberId RETURNING *""".query(decoder)

  val currentMemberActiveTimeSql: Query[MemberId, LocalDateTime] =
    sql"""SELECT active_time FROM members WHERE id = $memberId AND deleted = false""".query(timestamp)

  val selectExpiredMembers: Query[Void, Member] =
    sql"""SELECT * FROM members WHERE DATE(active_time) = DATE(NOW() - INTERVAL '3 DAY')""".query(decoder)

  val selectWeekLeftOnAT: Query[UserId, Member] =
    sql"""SELECT * FROM members WHERE user_id = $userId
      AND DATE(active_time - INTERVAL '7 DAY') < CURRENT_DATE
      AND CURRENT_DATE < DATE(active_time)""".query(decoder)

  val selectMemberByIdSql: Query[MemberId, Member] =
    sql"""SELECT * FROM members WHERE id = $memberId""".query(decoder)

}
