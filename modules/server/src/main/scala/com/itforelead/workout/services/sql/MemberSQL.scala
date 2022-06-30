package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.types._
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.codec.all.date
import skunk.implicits._

object MemberSQL {
  val memberId: Codec[MemberId] = identity[MemberId]

  private val Columns = memberId ~ userId ~ memberFirstName ~ memberLastName ~ tel ~ date ~ filePath

  val encoder: Encoder[MemberId ~ CreateMember] =
    Columns.contramap { case i ~ u =>
      i ~ u.userId ~ u.firstname ~ u.lastname ~ u.phone ~ u.birthday ~ u.image
    }

  val memberDecoder: Decoder[Member] =
    Columns.map { case i ~ ui ~ fn ~ ln ~ p ~ b ~ fp =>
      Member(i, ui, fn, ln, p, b, fp)
    }

  val selectByUserId: Query[UserId, Member] =
    sql"""SELECT * FROM members WHERE user_id = $userId""".query(memberDecoder)

  val insertMember: Query[MemberId ~ CreateMember, Member] =
    sql"""INSERT INTO members VALUES ($encoder) returning *""".query(memberDecoder)

}
