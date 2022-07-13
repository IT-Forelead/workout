package workout.services

import cats.effect.IO
import com.itforelead.workout.domain.custom.exception.MemberNotFound
import com.itforelead.workout.domain.types.MessageId
import com.itforelead.workout.services.{ArrivalService, Members, MessageBroker, Messages}
import cats.implicits.catsSyntaxApplicativeError
import com.itforelead.workout.domain.custom.refinements.{Tel, ValidationCode}
import workout.utils.DBSuite
import workout.utils.Generators.{createArrivalGen, createMemberGen, defaultFileKey, defaultUserId}

object ArrivalSuite extends DBSuite {

  test("Create Arrival") { implicit postgres =>
    val arrivalService                   = ArrivalService[IO]
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    val gen = for {
      a <- createArrivalGen
      m <- createMemberGen()
    } yield (a, m)

    forall(gen) { case (createArrival, createMember) =>
      for {
        _              <- members.sendValidationCode(defaultUserId, createMember.phone)
        validationCode <- RedisClient.get(createMember.phone.value)
        code = ValidationCode.unsafeFrom(validationCode.get)
        member1  <- members.validateAndCreate(defaultUserId, createMember.copy(code = code), defaultFileKey)
        arrival1 <- arrivalService.create(defaultUserId, createArrival.copy(memberId = member1.id))
        arrival2 <- arrivalService.get(defaultUserId)
      } yield assert(arrival2.exists(_.arrival == arrival1))
    }
  }

  test("Create Arrival: Member Not Found") { implicit postgres =>
    val arrivalService = ArrivalService[IO]
    forall(createArrivalGen) { createArrival =>
      arrivalService.create(defaultUserId, createArrival).as(failure(s"The test should return error")).recover {
        case _: MemberNotFound.type => success
        case error                  => failure(s"the test failed. $error")
      }
    }
  }
}
