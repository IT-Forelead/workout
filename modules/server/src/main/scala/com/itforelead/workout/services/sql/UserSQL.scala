package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.User.CreateUser
import skunk._
import skunk.implicits._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types._
import skunk.codec.all.date

object UserSQL {
  val userId: Codec[UserId] = identity[UserId]

  private val Columns            = userId ~ userName ~ userName ~ tel ~ gymName ~ passwordHash
  private val ColumnsWithoutPass = userId ~ userName ~ userName ~ tel ~ gymName

  val encoder: Encoder[UserId ~ CreateUser ~ PasswordHash[SCrypt]] =
    Columns.contramap { case i ~ u ~ p =>
      i ~ u.firstname ~ u.lastname ~ u.phone ~ u.gymName ~ p
    }

  val userDecoder: Decoder[User ~ PasswordHash[SCrypt]] =
    Columns.map { case i ~ fn ~ ln ~ p ~ gn ~ ps =>
      User(i, fn, ln, p, gn) ~ ps
    }

  val userDecoderWithoutPass: Decoder[User] =
    ColumnsWithoutPass.map { case i ~ fn ~ ln ~ p ~ gn =>
      User(i, fn, ln, p, gn)
    }

  val selectUser: Query[Tel, User ~ PasswordHash[SCrypt]] =
    sql"""SELECT * FROM users WHERE phone = $tel""".query(userDecoder)

  val insertUser: Query[UserId ~ CreateUser ~ PasswordHash[SCrypt], User ~ PasswordHash[SCrypt]] =
    sql"""INSERT INTO users VALUES ($encoder) returning *""".query(userDecoder)

}
