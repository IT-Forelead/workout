package workout.http.routes

import cats.effect.{IO, Sync}
import cats.implicits._
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.User.{CreateUser, UserWithPassword}
import com.itforelead.workout.domain.custom.refinements.{EmailAddress, Password}
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
      email: EmailAddress
    ): F[Option[UserWithPassword]] =
      if (user.email.equalsIgnoreCase(email))
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
      for {
        auth <- AuthMock[IO](users(user, newUser.password), RedisClient)
        (postData, shouldReturn) =
          if (conflict)
            (newUser.copy(email = user.email), Status.Conflict)
          else
            (newUser, Status.Created)
        req    = POST(postData, uri"/auth/user")
        routes = AuthRoutes[IO](auth).routes(usersMiddleware)
        res <- expectHttpStatus(routes, req)(shouldReturn)
      } yield res
    }
  }

  test("POST login") {
    val gen = for {
      u <- userGen
      c <- userCredentialGen
      b <- booleanGen
    } yield (u, c, b)

    forall(gen) { case (user, c, isCorrect) =>
      for {
        auth <- AuthMock[IO](users(user, c.password), RedisClient)
        (postData, shouldReturn) =
          if (isCorrect)
            (c.copy(email = user.email), Status.Ok)
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
      u <- userGen
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
