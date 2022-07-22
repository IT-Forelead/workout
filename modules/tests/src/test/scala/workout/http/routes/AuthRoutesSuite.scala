package workout.http.routes

import cats.effect.{IO, Sync}
import cats.implicits._
import com.itforelead.workout.domain.Role.ADMIN
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.User.{CreateClient, UserWithPassword}
import com.itforelead.workout.domain.custom.exception.{PhoneInUse, ValidationCodeExpired, ValidationCodeIncorrect}
import com.itforelead.workout.domain.custom.refinements.{Password, Tel}
import com.itforelead.workout.routes.{AuthRoutes, deriveEntityEncoder}
import com.itforelead.workout.services.Users
import eu.timepit.refined.auto.autoUnwrap
import org.http4s.Method.{GET, POST}
import org.http4s.client.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{AuthScheme, Credentials, Status}
import workout.utils.Generators.{booleanGen, createUserGen, userCredentialGen, userGen}
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import weaver.Expectations
import workout.stub_services.{AuthMock, UsersStub}
import workout.utils.HttpSuite

import scala.concurrent.duration.DurationInt

object AuthRoutesSuite extends HttpSuite {

  def users[F[_]: Sync](user: User, pass: Password, errorType: Option[String] = None): Users[F] = new UsersStub[F] {
    override def find(
      phoneNumber: Tel
    ): F[Option[UserWithPassword]] =
      if (user.phone.equalsIgnoreCase(phoneNumber))
        SCrypt.hashpw[F](pass).map { hash =>
          UserWithPassword(user, hash).some
        }
      else
        none[UserWithPassword].pure[F]

    override def create(
      userParam: CreateClient,
      password: PasswordHash[SCrypt]
    ): F[User] = errorType match {
      case None                            => Sync[F].delay(user)
      case Some("validationCodeExpired")   => ValidationCodeExpired(userParam.phone).raiseError[F, User]
      case Some("phoneInUse")              => PhoneInUse(userParam.phone).raiseError[F, User]
      case Some("validationCodeIncorrect") => ValidationCodeIncorrect(userParam.code).raiseError[F, User]
      case _ => Sync[F].raiseError(new Exception("Error occurred creating user. error type: Unknown"))
    }
  }

  def createUserReq(
    shouldReturn: Status,
    errorType: Option[String] = None
  ): AuthRoutesSuite.F[Expectations] = {
    val gen = for {
      u <- userGen(ADMIN)
      c <- createUserGen
    } yield (u, c)

    forall(gen) { case user -> newUser =>
      for {
        auth  <- AuthMock[IO](users(user, newUser.password, errorType), RedisClient)
        token <- authToken(user)
        req    = POST(newUser, uri"/auth/user").putHeaders(token)
        routes = AuthRoutes[IO](auth).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(shouldReturn)
      } yield res
    }
  }

  test("create user - 'Success'") {
    createUserReq(Status.Created)
  }

  test("create user validationCodeIncorrect - 'Fail'") {
    createUserReq(Status.NotAcceptable, "validationCodeIncorrect".some)
  }

  test("create user validationCodeExpired - 'Fail'") {
    createUserReq(Status.NotAcceptable, "validationCodeExpired".some)
  }

  test("create user validationPhoneInUse - 'Fail'") {
    createUserReq(Status.NotAcceptable, "phoneInUse".some)
  }

  test("create user Unknown Error - 'Fail'") {
    createUserReq(Status.BadRequest, "".some)
  }

  test("POST Login") {
    val gen = for {
      u <- userGen()
      c <- userCredentialGen
      b <- booleanGen
    } yield (u, c, b)

    forall(gen) { case (user, c, isCorrect) =>
      for {
        auth <- AuthMock[IO](users(user, c.password), RedisClient)
        (postData, shouldReturn) =
          if (isCorrect)
            (c.copy(phone = user.phone), Status.Ok)
          else
            (c, Status.Forbidden)
        req    = POST(postData, uri"/auth/login")
        routes = AuthRoutes[IO](auth).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(shouldReturn)
      } yield res
    }
  }

  test("User Logout") {
    val gen = for {
      u <- userGen()
      b <- booleanGen
    } yield (u, b)

    forall(gen) { case (user, isAuthed) =>
      for {
        token <- AuthMock.tokens[IO].flatMap(_.create)
        status <-
          if (isAuthed)
            RedisClient.put(token.value, user, 1.minute).as(Status.NoContent)
          else
            IO(Status.Forbidden)
        req = GET(uri"/auth/logout").putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token.value)))
        auth <- AuthMock[IO](new UsersStub[F], RedisClient)
        routes = AuthRoutes[IO](auth).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(status)
      } yield res
    }
  }
}
