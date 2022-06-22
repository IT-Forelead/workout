package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.User.CreateUser
import skunk._
import skunk.implicits._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types._
import com.itforelead.workout.domain.Role
import skunk.codec.all.timestamp

object UserSQL {
  val userId: Codec[UserId] = identity[UserId]

  private val Columns            = userId ~ userName ~ tel ~ timestamp ~ filePath ~ role ~ passwordHash
  private val ColumnsWithoutPass = userId ~ userName ~ tel ~ timestamp ~ filePath ~ role

  val encoder: Encoder[UserId ~ CreateUser ~ PasswordHash[SCrypt]] =
    Columns.contramap { case i ~ u ~ p =>
      i ~ u.fullname ~ u.phoneNumber ~ u.birthday ~ u.userPicture ~ Role.USER ~ p
    }

  val userDecoder: Decoder[User ~ PasswordHash[SCrypt]] =
    Columns.map { case i ~ fn ~ p ~ b ~ up ~ r ~ ps =>
      User(i, fn, p, b, up, r) ~ ps
    }

  val userDecoderWithoutPass: Decoder[User] =
    ColumnsWithoutPass.map { case i ~ fn ~ p ~ b ~ up ~ r =>
      User(i, fn, p, b, up, r)
    }

  val selectUser: Query[Tel, User ~ PasswordHash[SCrypt]] =
    sql"""SELECT * FROM users WHERE phone = $tel""".query(userDecoder)

  val insertUser: Query[UserId ~ CreateUser ~ PasswordHash[SCrypt], User ~ PasswordHash[SCrypt]] =
    sql"""INSERT INTO users VALUES ($encoder) returning *""".query(userDecoder)

}
