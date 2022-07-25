package e2e

import com.itforelead.workout.routes.{deriveEntityDecoder, deriveEntityEncoder}
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.auto.exportDecoder
import org.http4s.Method.{GET, POST}
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import workout.utils.ClientSuite
import workout.utils.Generators.validationGen

object MessageRoutesSuite extends ClientSuite {

  test("GET Messages E2E") { implicit resources =>
    for {
      token <- loginReq.expectAs[JwtToken]
      res <- GET(uri"/message")
        .putHeaders(makeAuth(token))
        .expectHttpStatus(Status.Ok)
    } yield res
  }

  test("Send Code E2E") { implicit resources =>
    forall(validationGen) { validation =>
      for {
        token <- loginReq.expectAs[JwtToken]
        result <- POST(validation, uri"/message/sent-code")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Ok)
      } yield result
    }
  }
}
