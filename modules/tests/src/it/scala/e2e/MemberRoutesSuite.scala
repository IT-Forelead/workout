package e2e
import com.itforelead.workout.domain._
import com.itforelead.workout.routes.{deriveEntityDecoder, deriveEntityEncoder}
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.Method.POST
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import workout.utils.ClientSuite
import workout.utils.Generators.validationGen

object MemberRoutesSuite extends ClientSuite {

  test("Send Code E2E") { implicit resources =>
    forall(validationGen) { validation =>
      for {
        token <- loginReq.expectAs[JwtToken]
        result <- POST(validation, uri"/member/sent-code")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Ok)
      } yield result
    }
  }

}
