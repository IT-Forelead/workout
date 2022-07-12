package e2e

import com.itforelead.workout.domain.types.MemberId
import com.itforelead.workout.routes.deriveEntityDecoder
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.auto.exportDecoder
import org.http4s.Method.{GET, POST}
import org.http4s.Status
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import workout.utils.Generators._
import workout.utils.ClientSuite

import java.util.UUID

object ArrivalRoutesSuite extends ClientSuite {

  test("Create Arrival E2E: Member Not Found") { implicit client =>
    forall(createArrivalGen) { createArrival =>
      for {
        token <- loginReq.expectAs[JwtToken]
        res <- POST(createArrival, uri"/arrival")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.NotFound)
      } yield res
    }
  }

  test("Create Arrival E2E") { implicit client =>
    forall(createArrivalGen) { createArrival =>
      for {
        token <- loginReq.expectAs[JwtToken]
        res <- POST(
          createArrival.copy(memberId = MemberId(UUID.fromString("99eb364c-f843-11ec-b939-0242ac120002"))),
          uri"/arrival"
        )
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Created)
      } yield res
    }
  }

  test("Get Arrival E2E") { implicit client =>
    forall(arrivalIdGen) { arrivalId =>
      for {
        token <- loginReq.expectAs[JwtToken]
        result <- GET(arrivalId, uri"/arrival")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Ok)
      } yield result
    }
  }

}
