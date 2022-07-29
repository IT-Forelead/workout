package workout.services

import cats.effect.IO
import eu.timepit.refined.auto.autoUnwrap
import cats.implicits.catsSyntaxApplicativeError
import com.itforelead.workout.domain.Role.CLIENT
import com.itforelead.workout.domain.User.UserActivate
import com.itforelead.workout.domain.custom.exception.PhoneInUse
import com.itforelead.workout.domain.custom.refinements.{Tel, ValidationCode}
import com.itforelead.workout.domain.types.MessageId
import com.itforelead.workout.services.{Members, MessageBroker, Messages, UserSettings, Users}
import tsec.passwordhashers.jca.SCrypt
import workout.stub_services.RedisClientMock
import workout.utils.DBSuite
import workout.utils.Generators.{createUserGen, defaultUserId, updateSettingGen, userFilterGen}

object UsersSuite extends DBSuite {

  test("Create Client") { implicit postgres =>
    val users                            = Users[IO](RedisClientMock.apply)
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    val gen = for {
      cu <- createUserGen
      f  <- userFilterGen
    } yield (cu, f)

    forall(gen) { case (createUser, filter) =>
      SCrypt.hashpw[IO](createUser.password).flatMap { hash =>
        for {
          _          <- messages.sendValidationCode(phone = createUser.phone)
          code       <- RedisClient.get(createUser.phone.value)
          client1    <- users.create(createUser.copy(code = ValidationCode.unsafeFrom(code.get)), hash)
          _          <- users.userActivate(UserActivate(client1.id))
          client2    <- users.find(client1.phone)
          getClients <- users.getClients(filter.copy(typeBy = None, sortBy = client2.get.user.activate), 1)
        } yield assert(getClients.user.exists(_.user == client2.get.user) && client2.get.user.role == CLIENT)
      }
    }
  }

  test("Create Client: Phone In Use") { implicit postgres =>
    val users                            = Users[IO](RedisClientMock.apply)
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    val gen = for {
      cu1 <- createUserGen
      cu2 <- createUserGen
    } yield (cu1, cu2)

    forall(gen) { case (createUser1, createUser2) =>
      SCrypt.hashpw[IO](createUser1.password).flatMap { hash =>
        (for {
          _       <- messages.sendValidationCode(phone = createUser1.phone)
          code1    <- RedisClient.get(createUser1.phone.value)
          client1 <- users.create(createUser1.copy(code = ValidationCode.unsafeFrom(code1.get)), hash)
          _       <- users.userActivate(UserActivate(client1.id))
          _       <- messages.sendValidationCode(phone = createUser1.phone)
          code2    <- RedisClient.get(createUser1.phone.value)
          _ <- users.create(createUser2.copy(code = ValidationCode.unsafeFrom(code2.get), phone = createUser1.phone), hash)
        } yield failure(s"The test should return error")).recover {
          case _: PhoneInUse => success
          case error         => failure(s"the test failed. $error")
        }
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
