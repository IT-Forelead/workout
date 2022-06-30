package workout.services

import cats.effect.IO
import com.itforelead.workout.services.{Payments, Users}
import eu.timepit.refined.auto.autoUnwrap
import tsec.passwordhashers.jca.SCrypt
import workout.utils.DBSuite
import workout.utils.Generators.{createPaymentGen, createUserGen}

object PaymentsSuite extends DBSuite {

  test("Create Payment") { implicit postgres =>
    val users    = Users[IO]
    val payments = Payments[IO]
    val gen = for {
      u  <- createUserGen
      cp <- createPaymentGen
    } yield (u, cp)
    forall(gen) { case (createUser, createPayment) =>
      for {
        hash <- SCrypt.hashpw[IO](createUser.password)
        user1 <- users.create(createUser, hash)
        payment <- payments.create(createPayment.copy(userId = user1.id))
        getPayments <- payments.payments(user1.id)
      } yield assert(getPayments.exists(p => p.payment == payment))
    }
  }
}
