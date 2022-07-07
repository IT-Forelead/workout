package workout.stub_services
import com.itforelead.workout.domain.{UserSetting, types}
import com.itforelead.workout.services.UserSettings

class UserSettingsStub[F[_]] extends UserSettings[F] {
  override def settings(userId: types.UserId): F[UserSetting]        = ???
  override def updateSettings(settings: UserSetting): F[UserSetting] = ???
}
