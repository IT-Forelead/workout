package workout.services

import cats.effect.IO
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxOptionId}
import com.itforelead.workout.domain.custom.exception.{PhoneInUse, ValidationCodeExpired, ValidationCodeIncorrect}
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel, ValidationCode}
import com.itforelead.workout.domain.types.MessageId
import com.itforelead.workout.services.{Members, MessageBroker, Messages}
import eu.timepit.refined.cats.refTypeShow
import workout.utils.DBSuite
import workout.utils.Generators.{createMemberGen, defaultFileKey, defaultUserId, phoneGen, validationCodeGen}

object MembersSuite extends DBSuite {

  test("Create Member") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    forall(createMemberGen()) { createMember =>
      for {
        _              <- members.sendValidationCode(defaultUserId, createMember.phone)
        validationCode <- RedisClient.get(createMember.phone.value)
        member1 <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode.get)),
          defaultFileKey
        )
        getMember <- members.findMemberByPhone(createMember.phone)
        membersList <- members.findByUserId(defaultUserId, 1)
      } yield assert(membersList.member.contains(member1)) && assert(getMember.get == member1)
    }
  }

  test("Create Member Phone In Use") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    val gen = for {
      m  <- createMemberGen()
      nm <- createMemberGen()
    } yield (m, nm)
    forall(gen) { case (createMember, createNewMember) =>
      (for {
        _               <- members.sendValidationCode(defaultUserId, createMember.phone)
        validationCode1 <- RedisClient.get(createMember.phone.value)
        member1 <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode1.get)),
          defaultFileKey
        )
        _ <- members.validateAndCreate(
          defaultUserId,
          createNewMember.copy(code = ValidationCode.unsafeFrom(validationCode1.get), phone = member1.phone),
          defaultFileKey
        )
      } yield failure(s"The test should return error")).recover {
        case _: PhoneInUse => success
        case error         => failure(s"the test failed. $error")
      }
    }
  }

  test("Create Member Validation Code Incorrect") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    val gen = for {
      m <- createMemberGen()
      c <- validationCodeGen
    } yield (m,c)
    forall(gen) { case (createMember, validationCode) =>
      (for {
        _               <- members.sendValidationCode(defaultUserId, createMember.phone)
        _ <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = validationCode),
          defaultFileKey
        )
      } yield failure(s"The test should return error")).recover {
        case _: ValidationCodeIncorrect => success
        case error         => failure(s"the test failed. $error")
      }
    }
  }

  test("Create Member Validation Code Expired") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    val gen = for {
      m  <- createMemberGen()
      p  <- phoneGen
    } yield (m, p)
    forall(gen) { case (createMember, phone) =>
      (for {
        _               <- members.sendValidationCode(defaultUserId, createMember.phone)
        validationCode1 <- RedisClient.get(createMember.phone.value)
        _ <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode1.get), phone = phone),
          defaultFileKey
        )
      } yield failure(s"The test should return error")).recover {
        case _: ValidationCodeExpired => success
        case error         => failure(s"the test failed. $error")
      }
    }
  }

}
