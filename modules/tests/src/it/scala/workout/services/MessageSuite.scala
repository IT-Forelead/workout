package workout.services

import cats.effect.IO
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxOptionId}
import com.itforelead.workout.domain.custom.exception.MemberNotFound
import com.itforelead.workout.domain.custom.refinements.{Tel, ValidationCode}
import com.itforelead.workout.domain.types.MessageId
import com.itforelead.workout.services.{Members, MessageBroker, Messages}
import workout.utils.DBSuite
import workout.utils.Generators.{
  createMemberGen,
  createMessageGen,
  defaultFileKey,
  defaultUserId,
  deliveryStatusGen,
  memberIdGen,
  messageFilterGen
}

object MessageSuite extends DBSuite {

  test("Create Message") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (_: MessageId, _: Tel, _: String) => IO.unit
    val members                          = Members[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker)

    val gen = for {
      cm <- createMessageGen(defaultUserId.some)
      m  <- createMemberGen()
      f  <- messageFilterGen
    } yield (cm, m, f)

    forall(gen) { case (createMessage, createMember, filter) =>
      for {
        _              <- messages.sendValidationCode(defaultUserId, createMember.phone)
        validationCode <- RedisClient.get(createMember.phone.value)
        code = ValidationCode.unsafeFrom(validationCode.get)
        member1      <- members.validateAndCreate(defaultUserId, createMember.copy(code = code), defaultFileKey)
        message1     <- messages.create(createMessage.copy(memberId = member1.id.some))
        message2     <- messages.get(message1.userId)
        getMessages  <- messages.getMessagesWithTotal(
          defaultUserId,
          filter.copy(
            typeBy = None,
            filterDateFrom = None,
            filterDateTo = None
          ),
          1
        )
        getMembersId <- messages.sentSMSTodayMemberIds
      } yield assert(
        message2.exists(tc => tc.message.userId == message1.userId) &&
        getMessages.messages.exists(_.message == message1) &&
        getMembersId.contains(member1.id)
      )
    }
  }

  test("Create Message: Member Not Found") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (_: MessageId, _: Tel, _: String) => IO.unit
    val messages = Messages[IO](RedisClient, messageBroker)

    val gen = for {
      memberId <- memberIdGen
      message  <- createMessageGen(defaultUserId.some)
    } yield message.copy(memberId = memberId.some)
    forall(gen) { createMessage =>
      messages.create(createMessage).as(failure(s"The test should return error")).recover {
        case _: MemberNotFound.type => success
        case error                  => failure(s"the test failed. $error")
      }
    }
  }

  test("Change status") { implicit postgres =>
    val messageBroker: MessageBroker[IO] = (_: MessageId, _: Tel, _: String) => IO.unit
    val members                          = Members[IO](RedisClient)
    val messages                         = Messages[IO](RedisClient, messageBroker)

    val gen = for {
      m  <- createMemberGen()
      cm <- createMessageGen(defaultUserId.some)
      s  <- deliveryStatusGen
    } yield (m, cm, s)

    forall(gen) { case (createMember, createMessage, statusGen) =>
      for {
        _              <- messages.sendValidationCode(defaultUserId, createMember.phone)
        validationCode <- RedisClient.get(createMember.phone.value)
        code = ValidationCode.unsafeFrom(validationCode.get)
        member1  <- members.validateAndCreate(defaultUserId, createMember.copy(code = code), defaultFileKey)
        message1 <- messages.create(createMessage.copy(memberId = member1.id.some))
        message2 <- messages.changeStatus(message1.id, statusGen)
      } yield assert(message2.deliveryStatus == statusGen)
    }
  }

}
