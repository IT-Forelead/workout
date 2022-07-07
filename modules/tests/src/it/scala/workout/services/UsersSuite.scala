package workout.services

import cats.effect.IO
import com.itforelead.workout.services.{UserSettings, Users}
import eu.timepit.refined.auto.autoUnwrap
import tsec.passwordhashers.jca.SCrypt
import workout.utils.DBSuite
import workout.utils.Generators.{createUserGen, defaultUserId, userSettingGen}

object UsersSuite extends DBSuite {

  test("Create User") { implicit postgres =>
    val users = Users[IO]
    forall(createUserGen) { createUser =>
      SCrypt.hashpw[IO](createUser.password).flatMap { hash =>
        for {
          user1 <- users.create(createUser, hash)
          user2 <- users.find(user1.phone)
        } yield assert(user2.exists(_.user == user1))
      }
    }
  }

  test("User settings by id") { implicit postgres =>
    val userSettings = UserSettings[IO]
    for {
      userSettings <- userSettings.settings(defaultUserId)
    } yield assert(userSettings.userId == defaultUserId)
  }

  test("Update user settings") { implicit postgres =>
    val userSettings = UserSettings[IO]
    forall(userSettingGen) { userSetting =>
      for {
        settings <- userSettings.updateSettings(userSetting)
      } yield assert(settings.userId == userSetting.userId)
    }
  }

}
