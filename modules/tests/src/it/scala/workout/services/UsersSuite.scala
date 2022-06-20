package workout.services

import cats.effect.IO
import com.itforelead.workout.services.Users
import eu.timepit.refined.auto.autoUnwrap
import workout.utils.Generators.createUserGen
import tsec.passwordhashers.jca.SCrypt
import workout.utils.DBSuite

object UsersSuite extends DBSuite {

  test("Create User") { implicit postgres =>
    val users = Users[IO]
    forall(createUserGen) { createUser =>
      SCrypt.hashpw[IO](createUser.password).flatMap { hash =>
        for {
          user1 <- users.create(createUser, hash)
          user2 <- users.find(user1.email)
        } yield assert(user2.exists(_.user == user1))
      }
    }
  }
}
