package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.Member
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types._
import skunk._
import skunk.codec.all.date
import skunk.implicits._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt

object MemberSQL {
  val memberId: Codec[MemberId] = identity[MemberId]
  val gymId: Codec[GymId] = identity[GymId]

  private val Columns            = memberId ~ gymId ~ userName ~ userName ~ tel ~ date ~ filePath ~ passwordHash
  private val ColumnsWithoutPass = memberId ~ gymId ~ userName ~ userName ~ tel ~ date ~ filePath

  val encoder: Encoder[MemberId ~ CreateMember ~ PasswordHash[SCrypt]] =
    Columns.contramap { case i ~ u ~ p =>
      i ~ u.gymId ~ u.firstname ~ u.lastname ~ u.phone ~ u.birthday ~ u.userPicture ~ p
    }

  val memberDecoder: Decoder[Member ~ PasswordHash[SCrypt]] =
    Columns.map { case i ~ gi ~ fn ~ ln ~ p ~ b ~ fp ~ ps =>
      Member(i, gi, fn, ln, p, b, fp) ~ ps
    }

  val memberDecoderWithoutPass: Decoder[Member] =
    ColumnsWithoutPass.map { case i ~ gi ~ fn ~ ln ~ p ~ b ~ fp =>
      Member(i, gi, fn, ln, p, b, fp)
    }

  val selectMember: Query[Tel, Member ~ PasswordHash[SCrypt]] =
    sql"""SELECT * FROM members WHERE phone = $tel""".query(memberDecoder)

  val insertMember: Query[MemberId ~ CreateMember ~ PasswordHash[SCrypt], Member ~ PasswordHash[SCrypt]] =
    sql"""INSERT INTO members VALUES ($encoder) returning *""".query(memberDecoder)

}
