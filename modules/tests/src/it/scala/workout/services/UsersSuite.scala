package workout.services

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import com.itforelead.workout.domain.Role.CLIENT
import com.itforelead.workout.services.{UserSettings, Users}
import eu.timepit.refined.auto.autoUnwrap
import tsec.passwordhashers.jca.SCrypt
import workout.utils.DBSuite
import workout.utils.Generators.{createUserGen, defaultUserId, updateSettingGen, userFilterGen}

object UsersSuite extends DBSuite {

  test("Create Client") { implicit postgres =>
    val users = Users[IO]
    val gen = for {
      cu <- createUserGen
      f  <- userFilterGen
    } yield (cu, f)

    forall(gen) { case (createUser, filter) =>
      SCrypt.hashpw[IO](createUser.password).flatMap { hash =>
        for {
          client1    <- users.create(createUser, hash)
          client2    <- users.find(client1.phone)
          getClients <- users.getClients(filter)
        } yield assert(getClients.contains(client2.get.user) && client2.get.user.role == CLIENT)
      }
    }
  }

  test("Settings By Id") { implicit postgres =>
    val userSettings = UserSettings[IO]
    userSettings.settings(defaultUserId).map(s => assert(s.userId == defaultUserId))
  }

  test("Update Settings") { implicit postgres =>
    val userSettings = UserSettings[IO]
    forall(updateSettingGen) { setting =>
      userSettings.updateSettings(defaultUserId, setting).map(s => assert(s.userId == defaultUserId))
    }
  }

}
