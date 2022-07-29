package e2e

import cats.Show.catsStdShowForTuple2
import cats.data.OptionT
import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import com.itforelead.workout.domain.User.UserActivate
import com.itforelead.workout.domain._
import com.itforelead.workout.domain.custom.refinements.ValidationCode
import com.itforelead.workout.implicits.CirceDecoderOps
import com.itforelead.workout.routes.{deriveEntityDecoder, deriveEntityEncoder}
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.Method.{GET, POST, PUT}
import org.http4s.Status
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import weaver.Expectations
import workout.utils.ClientSuite
import workout.utils.Generators.{createUserGen, updateSettingGen, userCredentialGen, userFilterGen}

object UserRoutesSuite extends ClientSuite {

  def createUserReq(
    shouldReturn: Status,
    errorType: Option[String] = None
  )(implicit resources: Res): UserRoutesSuite.F[Expectations] = {
    val gen = for {
      cu1 <- createUserGen
      cu2 <- createUserGen
    } yield (cu1, cu2)

    forall(gen) { case createUser1 -> createUser2 =>
      for {
        _    <- POST(Validation(createUser1.phone), uri"/message/public/sent-code").expectAs[Unit]
        code <- resources.redis.get(createUser1.phone.value)
        user1 = createUser1.copy(code = ValidationCode.unsafeFrom(code.get))
        user2 = createUser2.copy(code = ValidationCode.unsafeFrom(code.get), phone = user1.phone)
        _ <- if (errorType == "phoneInUse".some) POST(user1, uri"/auth/user").expectAs[JwtToken] else IO.unit
        result <- POST(user2, uri"/auth/user")
          .expectHttpStatus(shouldReturn)
      } yield result
    }
  }

  test("Get Clients") { implicit client =>
    forall(userFilterGen) { filter =>
      for {
        token <- loginReq.expectAs[JwtToken]
        result <- POST(filter, uri"/user/clients/1")
          .putHeaders(makeAuth(token))
          .expectHttpStatus(Status.Ok)
      } yield result
    }
  }

  test("Create Client") { implicit client =>
    createUserReq(Status.Created)
  }

  test("Create Client: Phone In Use") { implicit client =>
    createUserReq(Status.NotAcceptable, "phoneInUse".some)
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

  def testLoginReq(
    shouldReturn: Status,
    errorType: Option[String] = None
  )(implicit resources: Res): UserRoutesSuite.F[Expectations] = {
    val gen = for {
      u <- createUserGen
      p <- userCredentialGen
    } yield (u, p)

    forall(gen) { case (createUser, credentials) =>
      for {
        token <- loginReq.expectAs[JwtToken]
        _     <- POST(Validation(createUser.phone), uri"/message/public/sent-code").expectAs[Unit]
        code  <- resources.redis.get(createUser.phone.value)
        user = createUser.copy(code = ValidationCode.unsafeFrom(code.get))
        user1   <- POST(user, uri"/auth/user").expectAs[JwtToken]
        getUser <- OptionT(resources.redis.get(user1.value)).map(_.as[User]).value
        _       <- POST(UserActivate(getUser.get.id), uri"/user/activate").putHeaders(makeAuth(token)).expectAs[User]
        result <- POST(
          Credentials(
            phone = if (errorType == "userNotFound".some) credentials.phone else user.phone,
            password = if (errorType == "invalidPassword".some) credentials.password else user.password
          ),
          uri"/auth/login"
        ).expectHttpStatus(shouldReturn)
      } yield result
    }
  }

  test("Login") { implicit client =>
    testLoginReq(Status.Ok)
  }

  test("Login: User Not Found") { implicit client =>
    testLoginReq(Status.Forbidden, "userNotFound".some)
  }

  test("Login: Invalid Password") { implicit client =>
    testLoginReq(Status.Forbidden, "invalidPassword".some)
  }
}
