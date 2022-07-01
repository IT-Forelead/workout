package workout.services

import cats.effect.IO
import com.itforelead.workout.services.{Members, Payments, UserSettings, Users}
import eu.timepit.refined.auto.autoUnwrap
import tsec.passwordhashers.jca.SCrypt
import workout.utils.DBSuite
import workout.utils.Generators.{createMemberGen, createPaymentGen, createUserGen}

object PaymentsSuite extends DBSuite {

  test("Create Payment") { implicit postgres =>
    val users        = Users[IO]
    val members      = Members[IO]
    val userSettings = UserSettings[IO]
    val payments     = Payments[IO](userSettings)
    val gen = for {
      u  <- createUserGen
      m  <- createMemberGen
      cp <- createPaymentGen
    } yield (u, m, cp)
    forall(gen) { case (createUser, createMember, createPayment) =>
      for {
        hash        <- SCrypt.hashpw[IO](createUser.password)
        user1       <- users.create(createUser, hash)
        member1     <- members.create(createMember.copy(userId = user1.id))
        payment     <- payments.create(createPayment.copy(userId = user1.id, memberId = member1.id))
        getPayments <- payments.payments(user1.id)
      } yield assert(getPayments.exists(_.payment == payment))
    }
  }
}
