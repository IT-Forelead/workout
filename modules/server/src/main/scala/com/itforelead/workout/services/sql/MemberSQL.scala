package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel}
import com.itforelead.workout.domain.types._
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.codec.all.{bool, date, int8}
import skunk.implicits._

object MemberSQL {
  val memberId: Codec[MemberId] = identity[MemberId]

  private val Columns = memberId ~ userId ~ firstName ~ lastName ~ tel ~ date ~ fileKey ~ bool

  val encoder: Encoder[MemberId ~ UserId ~ CreateMember ~ FileKey] =
    Columns.contramap { case i ~ userId ~ m ~ key =>
      i ~ userId ~ m.firstname ~ m.lastname ~ m.phone ~ m.birthday ~ key ~ false
    }

  val decoder: Decoder[Member] =
    Columns.map { case i ~ ui ~ fn ~ ln ~ p ~ b ~ fp ~ _ =>
      Member(i, ui, fn, ln, p, b, fp)
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

  val insertMember: Query[MemberId ~ UserId ~ CreateMember ~ FileKey, Member] =
    sql"""INSERT INTO members VALUES ($encoder) RETURNING *""".query(decoder)

}
