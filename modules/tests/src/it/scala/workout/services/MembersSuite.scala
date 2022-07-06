package workout.services

import cats.effect.IO
import com.itforelead.workout.domain.custom.refinements.{FileKey, Tel, ValidationCode}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.{Members, Validations}
import eu.timepit.refined.cats.refTypeShow
import eu.timepit.refined.types.string.NonEmptyString
import workout.stub_services.MessageBrokerMock
import workout.utils.DBSuite
import workout.utils.Generators.{createMemberGen, phoneGen, userGen}

import java.util.UUID

object MembersSuite extends DBSuite {

  test("Create Member") { implicit postgres =>
    val messageBroker = new MessageBrokerMock[IO] {
      override def sendSMSWithoutMember(phone: Tel, text: NonEmptyString): IO[Unit] =
        IO.unit
    }
    val members     = Members[IO]
    val redis       = RedisClient
    val validations = Validations[IO](messageBroker, members, redis)

    val gen = for {
      p <- phoneGen
      m <- createMemberGen
    } yield (p, m)
    forall(gen) { case (phone, createMember) =>
      val userId  = UserId(UUID.fromString("76c2c44c-8fbf-4184-9199-19303a042fa0"))
      val fileKey = FileKey.unsafeFrom("e8bcab0c-ef16-45b5-842d-7ec35468195e.jpg")
      for {
        _              <- validations.sendValidationCode(phone)
        validationCode <- redis.get(phone.value)
        member1 <- validations.validatePhone(
          createMember.copy(userId = userId, phone = phone, code = ValidationCode.unsafeFrom(validationCode.get)),
          fileKey
        )
        members <- members.findByUserId(userId, 1)
      } yield assert(members.member.contains(member1))
    }
  }

}
