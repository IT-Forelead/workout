package e2e

import com.itforelead.workout.routes.deriveEntityDecoder
import dev.profunktor.auth.jwt.JwtToken
import io.circe.generic.auto.exportDecoder
import org.http4s.Method.GET
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sLiteralsSyntax
import workout.utils.ClientSuite

object MessageRoutesSuite extends ClientSuite {

  test("GET Messages E2E") { implicit client =>
    for {
      token <- loginReq.expectAs[JwtToken]
      res <- GET(uri"/message")
        .putHeaders(makeAuth(token))
        .expectHttpStatus(Status.Ok)
    } yield res
  }
}
