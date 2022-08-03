package com.itforelead.workout.services.sql

import com.itforelead.workout.domain.UserSetting
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.sql.UserSQL.userId
import skunk._
import skunk.implicits._

object UserSettingsSQL {
  private val Columns = userId ~ gymName ~ price ~ price

  val encoder: Encoder[UserSetting] =
    Columns.contramap { s =>
      s.userId ~ s.gymName ~ s.dailyPrice ~ s.monthlyPrice
    }

  val decoder: Decoder[UserSetting] =
    Columns.map {
      case ui ~ gn ~ dp ~ mp =>
        UserSetting(ui, gn, dp, mp)
    }

  val insertSettings: Query[UserSetting, UserSetting] =
    sql"""INSERT INTO user_settings VALUES ($encoder) returning *""".query(decoder)

  val selectSettings: Query[UserId, UserSetting] =
    sql"""SELECT * FROM user_settings WHERE user_id = $userId """.query(decoder)

  val updateUserSettings: Query[UserSetting, UserSetting] =
    sql"""UPDATE user_settings
            SET name = $gymName,
                daily_price = $price,
                monthly_price = $price
            WHERE user_id = $userId RETURNING *"""
      .query(decoder)
      .contramap[UserSetting](s => s.gymName ~ s.dailyPrice ~ s.monthlyPrice ~ s.userId)
}
