package workout.services

import cats.effect.IO
import com.itforelead.workout.domain.types.{MemberId, UserId}
import com.itforelead.workout.services.Messages
import workout.utils.DBSuite
import workout.utils.Generators.createMessageGen

import java.util.UUID

object MessageSuite extends DBSuite {

  test("Create Message") { implicit postgres =>
    val messages = Messages[IO]
    forall(createMessageGen) { createMessage =>
      for {
        message1 <- messages.create(createMessage.copy(
          userId = UserId(UUID.fromString("76c2c44c-8fbf-4184-9199-19303a042fa0")),
          memberId = MemberId(UUID.fromString("99eb364c-f843-11ec-b939-0242ac120002"))
        ))
        message2 <- messages.get(message1.userId)
      } yield assert(message2.exists(tc => tc.message.userId == message1.userId))
    }
  }

}
