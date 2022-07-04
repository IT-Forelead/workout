package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.Member.{CreateMember, MemberWithTotal}
import com.itforelead.workout.domain.custom.refinements.FileKey
import com.itforelead.workout.domain.types._
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.codec.all.{bool, date, int4}
import skunk.implicits._

object MemberSQL {
  val memberId: Codec[MemberId] = identity[MemberId]

  private val Columns = memberId ~ userId ~ firstName ~ lastName ~ tel ~ date ~ fileKey ~ bool

  val encoder: Encoder[MemberId ~ CreateMember ~ FileKey] =
    Columns.contramap { case i ~ u ~ key =>
      i ~ u.userId ~ u.firstname ~ u.lastname ~ u.phone ~ u.birthday ~ key ~ false
    }

  val memberDecoder: Decoder[Member] =
    Columns.map { case i ~ ui ~ fn ~ ln ~ p ~ b ~ fp ~ _ =>
      Member(i, ui, fn, ln, p, b, fp)
    }

  val memberDecoderWithTotal: Decoder[MemberWithTotal] =
    (memberId ~ userId ~ firstName ~ lastName ~ tel ~ date ~ fileKey ~ bool ~ int4).map {
      case i ~ ui ~ fn ~ ln ~ p ~ b ~ fp ~ _ ~ t =>
        MemberWithTotal(Member(i, ui, fn, ln, p, b, fp), t)
    }

  def selectByUserId(id: UserId, page: Int): AppliedFragment = {
    val filterByUserID: AppliedFragment =
      sql"""SELECT m.*, COUNT(m.*) FROM members m WHERE m.user_id = $userId AND m.deleted = false""".apply(id)
    filterByUserID.paginate(10, page)
  }

  val insertMember: Query[MemberId ~ CreateMember ~ FileKey, Member] = {
    sql"""INSERT INTO members VALUES ($encoder) RETURNING *""".query(memberDecoder)
  }

}
