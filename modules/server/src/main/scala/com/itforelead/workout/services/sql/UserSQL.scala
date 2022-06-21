package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.User.CreateUser
import skunk._
import skunk.implicits._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import com.itforelead.workout.domain.custom.refinements.{EmailAddress, Tel}
import com.itforelead.workout.domain.types._
import com.itforelead.workout.domain.Role
import skunk.codec.all.timestamp

object UserSQL {
  val userId: Codec[UserId] = identity[UserId]

  private val Columns = userId ~ userName ~ tel ~ timestamp ~ filePath ~ passwordHash ~ role

  val encoder: Encoder[UserId ~ CreateUser ~ PasswordHash[SCrypt]] =
    Columns.contramap { case i ~ u ~ p =>
      i ~ u.fullname ~ u.phoneNumber ~ u.birthday ~ u.userPicture ~ p ~ Role.USER
    }

  val decoder: Decoder[User ~ PasswordHash[SCrypt]] =
    Columns.map { case i ~ fn ~ p ~ b ~ up ~ ps ~ r =>
      User(i, fn, p, b, up, r) ~ ps
    }

  val selectUser: Query[Tel, User ~ PasswordHash[SCrypt]] =
    sql"""SELECT * FROM users WHERE phone = $phone""".query(decoder)

  val insertUser: Query[UserId ~ CreateUser ~ PasswordHash[SCrypt], User ~ PasswordHash[SCrypt]] =
    sql"""INSERT INTO users VALUES ($encoder) returning *""".query(decoder)

}
