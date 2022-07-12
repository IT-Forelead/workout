package e2e

import com.itforelead.workout.domain._
import com.itforelead.workout.routes.{deriveEntityDecoder, deriveEntityEncoder}
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.Method.{GET, PUT}
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import workout.utils.ClientSuite
import workout.utils.Generators.updateSettingGen

object UserRoutesSuite extends ClientSuite {

  test("User settings by id") { implicit client =>
    for {
      token <- loginReq().expectAs[JwtToken]
      result <- GET(uri"/user/settings")
        .putHeaders(makeAuth(token))
        .expectHttpStatus(Status.Ok)
    } yield result
  }

  test("Update user settings") { implicit client =>
    forall(updateSettingGen) { settings =>
      for {
        token <- loginReq().expectAs[JwtToken]
        result <- PUT(settings, uri"/user/settings")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Ok)
      } yield result
    }
  }

}
