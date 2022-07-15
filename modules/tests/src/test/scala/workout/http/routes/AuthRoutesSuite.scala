package workout.http.routes

import cats.effect.{IO, Sync}
import cats.implicits._
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.User.{CreateUser, UserWithPassword}
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
import workout.stub_services.{AuthMock, UsersStub}
import workout.utils.HttpSuite

import scala.concurrent.duration.DurationInt

object AuthRoutesSuite extends HttpSuite {

  def users[F[_]: Sync](user: User, pass: Password): Users[F] = new UsersStub[F] {
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
      userParam: CreateUser,
      password: PasswordHash[SCrypt]
    ): F[User] = user.pure[F]
  }

  test("POST create") {
    val gen = for {
      u <- userGen
      c <- createUserGen
      b <- booleanGen
    } yield (u, c, b)

    forall(gen) { case (user, newUser, conflict) =>
      val auth = AuthMock[IO](users(user, newUser.password), RedisClient)
      val (postData, shouldReturn) =
        if (conflict)
          (newUser.copy(phone = user.phone), Status.Conflict)
        else
          (newUser, Status.Created)
      val req    = POST(postData, uri"/auth/user")
      val routes = AuthRoutes[IO](auth).routes(usersMiddleware)
      expectHttpStatus(routes, req)(shouldReturn)
    }
  }

  test("POST login") {
    val gen = for {
      u <- userGen
      c <- userCredentialGen
      b <- booleanGen
    } yield (u, c, b)

    forall(gen) { case (user, c, isCorrect) =>
      val auth = AuthMock[IO](users(user, c.password), RedisClient)
      val (postData, shouldReturn) =
        if (isCorrect)
          (c.copy(phone = user.phone), Status.Ok)
        else
          (c, Status.Forbidden)
      val req    = POST(postData, uri"/auth/login")
      val routes = AuthRoutes[IO](auth).routes(usersMiddleware)
      expectHttpStatus(routes, req)(shouldReturn)
    }
  }

  test("User Logout") {
    val gen = for {
      u <- userGen
      b <- booleanGen
    } yield (u, b)

    forall(gen) { case (user, isAuthed) =>
      for {
        token <- AuthMock.tokens[IO].create
        status <-
          if (isAuthed)
            RedisClient.put(token.value, user, 1.minute).as(Status.NoContent)
          else
            IO(Status.Forbidden)
        req    = GET(uri"/auth/logout").putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token.value)))
        auth   = AuthMock[IO](new UsersStub[F], RedisClient)
        routes = AuthRoutes[IO](auth).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(status)
      } yield res
    }
  }
}
