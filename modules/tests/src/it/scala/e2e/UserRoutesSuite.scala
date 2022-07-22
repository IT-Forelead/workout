package e2e

import com.itforelead.workout.domain._
import com.itforelead.workout.routes.{deriveEntityDecoder, deriveEntityEncoder}
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.Method.{GET, POST, PUT}
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import workout.utils.ClientSuite
import workout.utils.Generators.{createUserGen, updateSettingGen}

object UserRoutesSuite extends ClientSuite {

  test("Create Client") { implicit client =>
    forall(createUserGen) { createUser =>
      for {
        token <- loginReq.expectAs[JwtToken]
        result <- POST(createUser, uri"/auth/user")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Created)
      } yield result
    }
  }

  test("Get Clients") { implicit client =>
    for {
      token <- loginReq.expectAs[JwtToken]
      result <- GET(uri"/user/clients")
        .putHeaders(makeAuth(token))
        .expectHttpStatus(Status.Ok)
    } yield result
  }

  test("Settings By Id") { implicit client =>
    for {
      token <- loginReq.expectAs[JwtToken]
      result <- GET(uri"/user/settings")
        .putHeaders(makeAuth(token))
        .expectHttpStatus(Status.Ok)
    } yield result
  }

  test("Update Settings") { implicit client =>
    forall(updateSettingGen) { settings =>
      for {
        token <- loginReq.expectAs[JwtToken]
        result <- PUT(settings, uri"/user/settings")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Ok)
      } yield result
    }
  }

}
