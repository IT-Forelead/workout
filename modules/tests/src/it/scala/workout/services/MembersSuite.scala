package workout.services

import cats.effect.{IO, Sync}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxOptionId}
import com.itforelead.workout.domain.Member.MemberFilter
import com.itforelead.workout.domain.custom.exception.{PhoneInUse, ValidationCodeExpired, ValidationCodeIncorrect}
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel, ValidationCode}
import com.itforelead.workout.domain.types.MessageId
import com.itforelead.workout.services.{Members, MessageBroker, Messages}
import eu.timepit.refined.cats.refTypeShow
import workout.services.MessageSuite.failure
import workout.utils.DBSuite
import workout.utils.Generators.{createMemberGen, defaultFileKey, defaultUserId, phoneGen, validationCodeGen}

import java.time.LocalDateTime

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
        getMember   <- members.findMemberByPhone(createMember.phone)
        membersList <- members.membersWithTotal(defaultUserId, MemberFilter(), 1)
      } yield assert(membersList.member.contains(member1)) && assert(getMember.get == member1)
    }
  }

  test("Create Member: Phone In Use") { implicit postgres =>
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

  test("Create Member: Validation Code Incorrect") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    val gen = for {
      m <- createMemberGen()
      c <- validationCodeGen
    } yield (m, c)
    forall(gen) { case (createMember, validationCode) =>
      (for {
        _ <- members.sendValidationCode(defaultUserId, createMember.phone)
        _ <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = validationCode),
          defaultFileKey
        )
      } yield failure(s"The test should return error")).recover {
        case _: ValidationCodeIncorrect => success
        case error                      => failure(s"the test failed. $error")
      }
    }
  }

  test("Create Member: Validation Code Expired") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    val gen = for {
      m <- createMemberGen()
      p <- phoneGen
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
        case error                    => failure(s"the test failed. $error")
      }
    }
  }

  test("Member By Id") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    val gen = for {
      p <- phoneGen
      m <- createMemberGen(p.some)
    } yield (p, m)
    forall(gen) { case (phone, createMember) =>
      val fileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
      for {
        _              <- members.sendValidationCode(defaultUserId, phone)
        validationCode <- RedisClient.get(phone.value)
        member <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode.get)),
          fileKey
        )
        memberDB <- members.findMemberById(member.id)
      } yield assert(memberDB.nonEmpty)
    }
  }

  test("Update member active time") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    val gen = for {
      p <- phoneGen
      m <- createMemberGen(p.some)
    } yield (p, m)
    forall(gen) { case (phone, createMember) =>
      val fileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
      for {
        _              <- members.sendValidationCode(defaultUserId, phone)
        validationCode <- RedisClient.get(phone.value)
        member <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode.get)),
          fileKey
        )
        memberDB <- members.updateActiveTime(member.id, LocalDateTime.now.minusDays(3))
        members  <- members.findActiveTimeShort
      } yield members match {
        case Nil            => failure(s"the test failed.")
        case ::(head, next) => success
      }
    }
  }

  test("Member By User Id") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    val gen = for {
      p <- phoneGen
      m <- createMemberGen(p.some)
    } yield (p, m)
    forall(gen) { case (phone, createMember) =>
      val fileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
      for {
        _              <- members.sendValidationCode(defaultUserId, phone)
        validationCode <- RedisClient.get(phone.value)
        _ <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode.get)),
          fileKey
        )
        membersDB <- members.get(defaultUserId)
      } yield assert(membersDB.nonEmpty)
    }
  }

  test("Member By Phone") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](messageBroker, Messages[IO], RedisClient)

    val gen = for {
      p <- phoneGen
      m <- createMemberGen(p.some)
    } yield (p, m)
    forall(gen) { case (phone, createMember) =>
      val fileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
      for {
        _              <- members.sendValidationCode(defaultUserId, phone)
        validationCode <- RedisClient.get(phone.value)
        _ <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode.get)),
          fileKey
        )
        memberDB <- members.findMemberByPhone(phone)
      } yield assert(memberDB.nonEmpty)
    }
  }

}
