package workout.stub_services

import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.domain.UserSetting
import com.itforelead.workout.domain.UserSetting.UpdateSetting
import com.itforelead.workout.services.UserSettings

class UserSettingsMock[F[_]] extends UserSettings[F] {
  override def settings(userId: UserId): F[UserSetting] = ???
  override def updateSettings(userId: UserId, settings: UpdateSetting): F[UserSetting] = ???

}
