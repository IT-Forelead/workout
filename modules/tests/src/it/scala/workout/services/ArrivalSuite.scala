package workout.services

import cats.effect.IO
import com.itforelead.workout.domain.types.MemberId
import com.itforelead.workout.services.ArrivalService
import workout.utils.DBSuite
import workout.utils.Generators.{createArrivalGen, defaultUserId}

import java.util.UUID

object ArrivalSuite extends DBSuite {

  test("Create Arrival") { implicit postgres =>
    val arrivalService = ArrivalService[IO]
    forall(createArrivalGen) { createArrival =>
      for {
        arrival1 <- arrivalService.create(
          defaultUserId,
          createArrival.copy(
            memberId = MemberId(UUID.fromString("99eb364c-f843-11ec-b939-0242ac120002"))
          )
        )
        arrival2 <- arrivalService.get(arrival1.userId)
      } yield assert(arrival2.contains(arrival1))
    }
  }

}
