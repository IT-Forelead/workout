package workout.services

import cats.effect.IO
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel, ValidationCode}
import com.itforelead.workout.domain.types.MessageId
import com.itforelead.workout.services._
import workout.utils.DBSuite
import workout.utils.Generators.{createMemberGen, createPaymentGen, defaultUserId}

object PaymentsSuite extends DBSuite {

  test("Create Payment") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit

    val members      = Members[IO](messageBroker, Messages[IO], RedisClient)
    val userSettings = UserSettings[IO]
    val payments     = Payments[IO](userSettings)
    val gen = for {
      m  <- createMemberGen()
      cp <- createPaymentGen
    } yield (m, cp)
    forall(gen) { case (createMember, createPayment) =>
      for {
        _              <- members.sendValidationCode(defaultUserId, createMember.phone)
        validationCode <- RedisClient.get(createMember.phone.value)
        member1 <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode.get)),
          FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
        )
        payment     <- payments.create(defaultUserId, createPayment.copy(memberId = member1.id))
        getPayments <- payments.payments(defaultUserId)
      } yield assert(getPayments.exists(_.payment == payment))
    }
  }
}
