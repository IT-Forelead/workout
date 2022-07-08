package workout.services

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
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
    userSettings.settings(defaultUserId).map(s => assert(s.userId == defaultUserId))
  }

  test("Update user settings") { implicit postgres =>
    val userSettings = UserSettings[IO]
    forall(userSettingGen(defaultUserId.some)) { userSetting =>
      userSettings.updateSettings(userSetting).map(s => assert(s.userId == userSetting.userId))
    }
  }

}
