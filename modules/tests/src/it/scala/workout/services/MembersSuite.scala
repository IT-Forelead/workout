package workout.services

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel, ValidationCode}
import com.itforelead.workout.domain.types.MessageId
import com.itforelead.workout.services.{Members, MessageBroker, Messages}
import eu.timepit.refined.cats.refTypeShow
import workout.utils.DBSuite
import workout.utils.Generators.{createMemberGen, defaultUserId, phoneGen}

import java.time.LocalDateTime

object MembersSuite extends DBSuite {

  test("Create Member") { implicit postgres =>
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
        member1 <- members.validateAndCreate(
          defaultUserId,
          createMember.copy(code = ValidationCode.unsafeFrom(validationCode.get)),
          fileKey
        )
        members <- members.findByUserId(defaultUserId, 1)
      } yield assert(members.member.contains(member1))
    }
  }

  test("Member by id") { implicit postgres =>
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
        memberDB <- members.updateActiveTime(member.id, LocalDateTime.now())
      } yield assert(memberDB.id == member.id)
    }
  }

}
