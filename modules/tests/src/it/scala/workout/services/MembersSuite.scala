package workout.services

import cats.effect.IO
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxOptionId}
import com.itforelead.workout.domain.Member.MemberFilter
import com.itforelead.workout.domain.custom.exception.{PhoneInUse, ValidationCodeExpired, ValidationCodeIncorrect}
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel, ValidationCode}
import com.itforelead.workout.domain.types.MessageId
import com.itforelead.workout.services.{Members, MessageBroker, Messages, Users}
import eu.timepit.refined.cats.refTypeShow
import workout.utils.DBSuite
import workout.utils.Generators._

import java.time.LocalDateTime

object MembersSuite extends DBSuite {

  test("Create Member") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](RedisClient)
    val users                            = Users[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    forall(createMemberGen()) { createMember =>
      for {
        _              <- messages.sendValidationCode(defaultUserId.some, createMember.phone)
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
    val members                          = Members[IO](RedisClient)
    val users                            = Users[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    val gen = for {
      m  <- createMemberGen()
      nm <- createMemberGen()
    } yield (m, nm)
    forall(gen) { case (createMember, createNewMember) =>
      (for {
        _               <- messages.sendValidationCode(defaultUserId.some, createMember.phone)
        validationCode1 <- RedisClient.get(createMember.phone.value)
        member1 <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode1.get)),
          defaultFileKey
        )
        _               <- messages.sendValidationCode(defaultUserId.some, createMember.phone)
        validationCode2 <- RedisClient.get(createMember.phone.value)
        _ <- members.validateAndCreate(
          defaultUserId,
          createNewMember.copy(code = ValidationCode.unsafeFrom(validationCode2.get), phone = member1.phone),
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
    val members                          = Members[IO](RedisClient)
    val users                            = Users[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    val gen = for {
      m <- createMemberGen()
      c <- validationCodeGen
    } yield (m, c)
    forall(gen) { case (createMember, validationCode) =>
      (for {
        _ <- messages.sendValidationCode(defaultUserId.some, createMember.phone)
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
    val members                          = Members[IO](RedisClient)
    val users                            = Users[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    val gen = for {
      m <- createMemberGen()
      p <- phoneGen
    } yield (m, p)
    forall(gen) { case (createMember, phone) =>
      (for {
        _               <- messages.sendValidationCode(defaultUserId.some, createMember.phone)
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
    val members                          = Members[IO](RedisClient)
    val users                            = Users[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    val gen = for {
      p <- phoneGen
      m <- createMemberGen(p.some)
    } yield (p, m)
    forall(gen) { case (phone, createMember) =>
      val fileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
      for {
        _              <- messages.sendValidationCode(defaultUserId.some, phone)
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
    val members                          = Members[IO](RedisClient)
    val users                            = Users[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    val gen = for {
      p <- phoneGen
      m <- createMemberGen(p.some)
    } yield (p, m)
    forall(gen) { case (phone, createMember) =>
      val fileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
      for {
        _              <- messages.sendValidationCode(defaultUserId.some, phone)
        validationCode <- RedisClient.get(phone.value)
        member <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode.get)),
          fileKey
        )
        _ <- members.updateActiveTime(member.id, LocalDateTime.now.minusDays(3))
        members  <- members.findActiveTimeShort
      } yield members match {
        case Nil            => failure(s"the test failed.")
        case ::(head, next) => success
      }
    }
  }

  test("Member By User Id") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](RedisClient)
    val users                            = Users[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    val gen = for {
      p <- phoneGen
      m <- createMemberGen(p.some)
    } yield (p, m)
    forall(gen) { case (phone, createMember) =>
      val fileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
      for {
        _              <- messages.sendValidationCode(defaultUserId.some, phone)
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
    val members                          = Members[IO](RedisClient)
    val users                            = Users[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    val gen = for {
      p <- phoneGen
      m <- createMemberGen(p.some)
    } yield (p, m)
    forall(gen) { case (phone, createMember) =>
      val fileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
      for {
        _              <- messages.sendValidationCode(defaultUserId.some, phone)
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

  test("Get members with total") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](RedisClient)
    val users                            = Users[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    val gen = for {
      p <- phoneGen
      m <- createMemberGen(p.some)
    } yield (p, m)
    forall(gen) { case (phone, createMember) =>
      val fileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
      for {
        _              <- messages.sendValidationCode(defaultUserId.some, phone)
        validationCode <- RedisClient.get(phone.value)
        _ <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode.get)),
          fileKey
        )
        memberDB <- members.membersWithTotal(defaultUserId, MemberFilter.apply(), 1)
      } yield assert(memberDB.member.nonEmpty)
    }
  }

  test("Get active time expired 7 days ago members by userId") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (messageId: MessageId, phone: Tel, text: String) => IO.unit
    val members                          = Members[IO](RedisClient)
    val users                            = Users[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker, users)

    val gen = for {
      p <- phoneGen
      m <- createMemberGen(p.some)
    } yield (p, m)
    forall(gen) { case (phone, createMember) =>
      val fileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
      for {
        _              <- messages.sendValidationCode(defaultUserId.some, phone)
        validationCode <- RedisClient.get(phone.value)
        member <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode.get)),
          fileKey
        )
        _ <- members.updateActiveTime(member.id, LocalDateTime.now.plusDays(3))
        members  <- members.getWeekLeftOnAT(defaultUserId)
      } yield assert(members.nonEmpty)
    }
  }

}
