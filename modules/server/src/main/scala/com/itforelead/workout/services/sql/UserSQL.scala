package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.{Role, User}
import com.itforelead.workout.domain.User.CreateClient
import skunk._
import skunk.implicits._
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types._
import skunk.codec.all.bool

object UserSQL {
  val userId: Codec[UserId] = identity[UserId]

  private val Columns                = userId ~ firstName ~ lastName ~ tel ~ passwordHash ~ role ~ bool ~ bool
  private val ColumnsWithoutPassword = userId ~ firstName ~ lastName ~ tel ~ role

  val encoder: Encoder[UserId ~ CreateClient ~ PasswordHash[SCrypt]] =
    Columns.contramap { case i ~ u ~ p =>
      i ~ u.firstname ~ u.lastname ~ u.phone ~ p ~ Role.CLIENT ~ false ~ false
    }

  val usersDecoder: Decoder[User] =
    ColumnsWithoutPassword.map { case i ~ fn ~ ln ~ p ~ r =>
      User(i, fn, ln, p, r)
    }

  val userDecoder: Decoder[User ~ PasswordHash[SCrypt]] =
    Columns.map { case i ~ fn ~ ln ~ p ~ ps ~ r ~ _ ~ _ =>
      User(i, fn, ln, p, r) ~ ps
    }

  val selectUser: Query[Tel, User ~ PasswordHash[SCrypt]] =
    sql"""SELECT * FROM users WHERE phone = $tel""".query(userDecoder)

  val selectClients: Query[Void, User] =
    sql"""SELECT id, firstname, lastname, phone, role FROM users WHERE role = 'client' """.query(usersDecoder)

  val insertUser: Query[UserId ~ CreateClient ~ PasswordHash[SCrypt], User ~ PasswordHash[SCrypt]] =
    sql"""INSERT INTO users VALUES ($encoder) returning *""".query(userDecoder)

}
