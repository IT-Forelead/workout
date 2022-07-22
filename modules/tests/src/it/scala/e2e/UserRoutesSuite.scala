package e2e

import com.itforelead.workout.domain.User.CreateClient
import com.itforelead.workout.domain._
import com.itforelead.workout.domain.custom.refinements.ValidationCode
import com.itforelead.workout.routes.{deriveEntityDecoder, deriveEntityEncoder}
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.Method.{GET, POST, PUT}
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import weaver.Expectations
import workout.utils.ClientSuite
import workout.utils.Generators.{createUserGen, updateSettingGen}

object UserRoutesSuite extends ClientSuite {

  def createUserReq(
    shouldReturn: Status
  )(implicit resources: Res): PaymentRoutesSuite.F[Expectations] = {
    forall(createUserGen) { createUser =>
      for {
        token <- loginReq.expectAs[JwtToken]
        _     <- POST(Validation(createUser.phone), uri"/message/sent-code").putHeaders(makeAuth(token)).expectAs[Unit]
        code <- resources.redis.get(createUser.phone.value)
        user = createUser.copy(code = ValidationCode.unsafeFrom(code.get))
        result <- POST(user, uri"/auth/user")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(shouldReturn)
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

  test("Create client") { implicit client =>
    UserRoutesSuite.createUserReq(Status.Created)
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
