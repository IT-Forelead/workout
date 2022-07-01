package workout.services

import cats.effect.IO
import com.itforelead.workout.domain.types.{MemberId, UserId}
import com.itforelead.workout.services.ArrivalService
import workout.utils.DBSuite
import workout.utils.Generators.createArrivalGen

import java.util.UUID

object ArrivalSuite extends DBSuite {

  test("Create Arrival") { implicit postgres =>
    val arrivalService = ArrivalService[IO]
    forall(createArrivalGen) { createArrival =>
      for {
        arrival1 <- arrivalService.create(createArrival.copy(
          userId = UserId(UUID.fromString("76c2c44c-8fbf-4184-9199-19303a042fa0")),
          memberId = MemberId(UUID.fromString("99eb364c-f843-11ec-b939-0242ac120002"))
        ))
        arrival2 <- arrivalService.get(arrival1.userId)
      } yield assert(arrival2.contains(arrival1))
    }
  }

}
