package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.{Role, User, UserFilterBy}
import com.itforelead.workout.domain.User.{CreateClient, UserWithSetting}
import com.itforelead.workout.domain.UserFilterBy._
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
  private val ColumnsWithoutPassword = userId ~ firstName ~ lastName ~ tel ~ role ~ bool

  val encoder: Encoder[UserId ~ CreateClient ~ PasswordHash[SCrypt]] =
    Columns.contramap { case i ~ u ~ p =>
      i ~ u.firstname ~ u.lastname ~ u.phone ~ p ~ Role.CLIENT ~ false ~ false
    }

  val decoder: Decoder[User] =
    ColumnsWithoutPassword.map { case i ~ fn ~ ln ~ p ~ r ~ ac =>
      User(i, fn, ln, p, r, ac)
    }

  val decoderWithPassword: Decoder[User ~ PasswordHash[SCrypt]] =
    Columns.map { case i ~ fn ~ ln ~ p ~ ps ~ r ~ _ ~ ac =>
      User(i, fn, ln, p, r, ac) ~ ps
    }

  private val userWithSettingColumns = decoder ~ UserSettingsSQL.decoder

  val decUserWithSetting: Decoder[UserWithSetting] =
    userWithSettingColumns.map { case user ~ setting =>
      UserWithSetting(user, setting)
    }

  val selectUser: Query[Tel, User ~ PasswordHash[SCrypt]] =
    sql"""SELECT * FROM users WHERE phone = $tel""".query(decoderWithPassword)

  val selectClients: Query[Boolean, UserWithSetting] =
    sql"""SELECT users.id, users.firstname, users.lastname, users.phone, users.role, user_settings.*
          FROM users
          INNER JOIN user_settings ON users.id = user_settings.user_id
         WHERE users.role = 'client' AND users.actived = $bool """.query(decUserWithSetting)

  def selectClientsFilter(filter: Option[UserFilterBy], activate: Boolean): AppliedFragment = {
    val filterBy = filter
      .fold("") {
        case FirstnameAZ => "ORDER BY users.firstname ASC"
        case FirstnameZA => "ORDER BY users.firstname DESC"
        case LastnameAZ  => "ORDER BY users.lastname ASC"
        case LastnameZA  => "ORDER BY users.lastname DESC"
      }

    sql"""SELECT users.id, users.firstname, users.lastname, users.phone, users.role, users.activate, user_settings.*
           FROM users
           INNER JOIN user_settings ON users.id = user_settings.user_id
           WHERE users.role = 'client' AND users.activate = $bool #$filterBy""".apply(activate)
  }

  val selectAdmin: Query[Void, User] =
    sql"""SELECT id, firstname, lastname, phone, role, activate FROM users WHERE role = 'admin' """.query(decoder)

  val insertUser: Query[UserId ~ CreateClient ~ PasswordHash[SCrypt], User ~ PasswordHash[SCrypt]] =
    sql"""INSERT INTO users VALUES ($encoder) returning *""".query(decoderWithPassword)

}
