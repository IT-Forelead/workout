package workout.services

import cats.effect.IO
import com.itforelead.workout.domain.custom.refinements.FileKey
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.{Members, Payments, UserSettings, Users}
import eu.timepit.refined.auto.autoUnwrap
import tsec.passwordhashers.jca.SCrypt
import workout.utils.DBSuite
import workout.utils.Generators.{createMemberGen, createPaymentGen, createUserGen, userGen}

import java.util.UUID

object PaymentsSuite extends DBSuite {

  test("Create Payment") { implicit postgres =>
    val members      = Members[IO]
    val userSettings = UserSettings[IO]
    val payments     = Payments[IO](userSettings)
    val gen = for {
      m  <- createMemberGen
      cp <- createPaymentGen
    } yield (m, cp)
    forall(gen) { case (createMember, createPayment) =>
      val userId = UserId(UUID.fromString("76c2c44c-8fbf-4184-9199-19303a042fa0"))
      for {
        member1 <- members.create(
          createMember.copy(userId = userId),
          filePath = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
        )
        payment     <- payments.create(createPayment.copy(userId = userId, memberId = member1.id))
        getPayments <- payments.payments(userId)
      } yield assert(getPayments.exists(_.payment == payment))
    }
  }
}
