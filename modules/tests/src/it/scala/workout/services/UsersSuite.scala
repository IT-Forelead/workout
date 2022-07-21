package workout.services

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import com.itforelead.workout.domain.Role.CLIENT
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.domain.types.{MessageId, UserId}
import com.itforelead.workout.services.{Members, MessageBroker, Messages, UserSettings, Users}
import eu.timepit.refined.auto.autoUnwrap
import tsec.passwordhashers.jca.SCrypt
import workout.stub_services.RedisClientMock
import workout.utils.DBSuite
import workout.utils.Generators.{createUserGen, defaultUserId, updateSettingGen}

import java.util.UUID

object UsersSuite extends DBSuite {

  test("Create Client") { implicit postgres =>
    val users = Users[IO](RedisClientMock.apply)
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    forall(createUserGen) { createUser =>
      SCrypt.hashpw[IO](createUser.password).flatMap { hash =>
        for {
          _ <- members.sendValidationCode(UserId(UUID.randomUUID()), createUser.phone)
          client1    <- users.create(createUser, hash)
          client2    <- users.find(client1.phone)
          getClients <- users.getClients
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
