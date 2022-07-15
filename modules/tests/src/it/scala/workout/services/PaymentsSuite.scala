package workout.services

import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeError
import com.itforelead.workout.domain.PaymentType.{DAILY, MONTHLY}
import com.itforelead.workout.domain.custom.exception.{CreatePaymentDailyTypeError, MemberNotFound}
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
    val payments     = Payments[IO](userSettings, members)
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
        getPayment  <- payments.getPaymentByMemberId(defaultUserId, payment.memberId)
      } yield assert(getPayments.exists(_.payment == payment) && getPayment.contains(payment))
    }
  }

  test("Create Payment: Member Not Found") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)
    val userSettings                     = UserSettings[IO]
    val payments                         = Payments[IO](userSettings, members)
    forall(createPaymentGen) { createPayment =>
      payments.create(defaultUserId, createPayment).as(failure(s"The test should return error")).recover {
        case _: MemberNotFound.type => success
        case error                  => failure(s"the test failed. $error")
      }
    }
  }

  test("Create Payment: Daily Type Error") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit

    val members      = Members[IO](messageBroker, Messages[IO], RedisClient)
    val userSettings = UserSettings[IO]
    val payments     = Payments[IO](userSettings, members)
    val gen = for {
      m  <- createMemberGen()
      cp <- createPaymentGen
    } yield (m, cp)
    forall(gen) { case (createMember, createPayment) =>
      (for {
        _              <- members.sendValidationCode(defaultUserId, createMember.phone)
        validationCode <- RedisClient.get(createMember.phone.value)
        member1 <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode.get)),
          FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
        )
        _ <- payments.create(defaultUserId, createPayment.copy(memberId = member1.id, paymentType = MONTHLY))
        _ <- payments.create(defaultUserId, createPayment.copy(memberId = member1.id, paymentType = DAILY))
      } yield failure(s"The test should return error")).recover {
        case _: CreatePaymentDailyTypeError.type => success
        case error                               => failure(s"the test failed. $error")
      }
    }
  }

}
