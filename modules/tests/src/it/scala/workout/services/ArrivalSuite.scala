package workout.services

import cats.effect.IO
import com.itforelead.workout.domain.custom.exception.MemberNotFound
import com.itforelead.workout.domain.types.MessageId
import com.itforelead.workout.services.{ArrivalService, Members, MessageBroker, Messages}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxOptionId}
import com.itforelead.workout.domain.Arrival.ArrivalFilter
import com.itforelead.workout.domain.custom.refinements.{Tel, ValidationCode}
import eu.timepit.refined.types.string.NonEmptyString
import weaver.Expectations
import workout.utils.DBSuite
import workout.utils.Generators.{arrivalFilterGen, createArrivalGen, createMemberGen, defaultFileKey, defaultUserId}

object ArrivalSuite extends DBSuite {

  test("Create Arrival: Member Not Found") { implicit postgres =>
    val arrivalService = ArrivalService[IO]
    forall(createArrivalGen) { createArrival =>
      arrivalService.create(defaultUserId, createArrival).as(failure(s"The test should return error")).recover {
        case _: MemberNotFound.type => success
        case error                  => failure(s"the test failed. $error")
      }
    }
  }

  def createArrival(methodName: String)(implicit res: Res): ArrivalSuite.F[Expectations] = {
    val arrivalService                   = ArrivalService[IO]
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    val gen = for {
      a <- createArrivalGen
      m <- createMemberGen()
      f <- arrivalFilterGen
    } yield (a, m, f)

    forall(gen) { case (createArrival, createMember, filter) =>
      for {
        _              <- members.sendValidationCode(defaultUserId, createMember.phone)
        validationCode <- RedisClient.get(createMember.phone.value)
        code = ValidationCode.unsafeFrom(validationCode.get)
        member1     <- members.validateAndCreate(defaultUserId, createMember.copy(code = code), defaultFileKey)
        arrival1    <- arrivalService.create(defaultUserId, createArrival.copy(memberId = member1.id))
        arrival2    <- arrivalService.get(defaultUserId)
        getArrivals <- arrivalService.getArrivalWithTotal(defaultUserId, filter.copy(typeBy = arrival1.arrivalType.some), 1)
        getArrival  <- arrivalService.getArrivalByMemberId(defaultUserId, arrival1.memberId)
      } yield methodName match {
        case "createArrival"        => assert(arrival2.exists(_.arrival == arrival1))
        case "getArrivalWithTotal"  => assert(getArrivals.arrival.exists(_.arrival == arrival1))
        case "getArrivalByMemberId" => assert(getArrival.contains(arrival1))
      }
    }
  }

  test("Create Arrival") { implicit postgres =>
    createArrival("createArrival")
  }

  test("Get Arrival With Total") { implicit postgres =>
    createArrival("getArrivalWithTotal")
  }

  test("Get Arrival By Member Id") { implicit postgres =>
    createArrival("getArrivalByMemberId")
  }

}
