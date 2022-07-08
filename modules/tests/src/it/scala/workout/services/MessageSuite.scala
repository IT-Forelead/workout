package workout.services

import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import com.itforelead.workout.domain.types.MemberId
import com.itforelead.workout.services.Messages
import workout.utils.DBSuite
import workout.utils.Generators.{createMessageGen, defaultUserId, deliveryStatusGen}

import java.util.UUID

object MessageSuite extends DBSuite {

  test("Create Message") { implicit postgres =>
    val messages = Messages[IO]
    forall(createMessageGen(defaultUserId.some)) { createMessage =>
      for {
        message1 <- messages.create(
          createMessage.copy(
            memberId = MemberId(UUID.fromString("99eb364c-f843-11ec-b939-0242ac120002")).some
          )
        )
        message2 <- messages.get(message1.userId)
      } yield assert(message2.exists(tc => tc.message.userId == message1.userId))
    }
  }

  test("Change status") { implicit postgres =>
    val messages = Messages[IO]
    val gen = for {
      cm <- createMessageGen(defaultUserId.some)
      s <- deliveryStatusGen
    } yield cm -> s

    forall(gen) { case createMessage -> statusGen =>
      for {
        message1 <- messages.create(
          createMessage.copy(
            memberId = MemberId(UUID.fromString("99eb364c-f843-11ec-b939-0242ac120002")).some
          )
        )
        message2 <- messages.changeStatus(message1.id, statusGen)
      } yield assert(message2.deliveryStatus == statusGen)
    }
  }

}
