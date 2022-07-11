package com.itforelead.workout.services

import cats.effect.{Resource, Sync}
import com.itforelead.workout.domain.UserSetting
import com.itforelead.workout.domain.UserSetting.UpdateSetting
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services.sql.UserSettingsSQL.{selectSettings, updateUserSettings}
import skunk.Session

trait UserSettings[F[_]] {

  def settings(userId: UserId): F[UserSetting]
  def updateSettings(userId: UserId, settings: UpdateSetting): F[UserSetting]

}

object UserSettings {
  def apply[F[_]: GenUUID: Sync](implicit
    session: Resource[F, Session[F]]
  ): UserSettings[F] =
    new UserSettings[F] with SkunkHelper[F] {

      override def settings(userId: UserId): F[UserSetting] =
        prepQueryUnique(selectSettings, userId)

      override def updateSettings(userId: UserId, settings: UpdateSetting): F[UserSetting] =
        prepQueryUnique(
          updateUserSettings,
          UserSetting(
            userId = userId,
            gymName = settings.gymName,
            dailyPrice = settings.dailyPrice,
            monthlyPrice = settings.monthlyPrice
          )
        )

    }
}
