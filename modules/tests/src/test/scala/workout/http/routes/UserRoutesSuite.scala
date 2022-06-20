package workout.http.routes

import cats.effect.IO
import cats.implicits._
import com.itforelead.workout.routes.UserRoutes
import org.http4s.Method.GET
import org.http4s.client.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{AuthScheme, Credentials, Status}
import workout.utils.Generators._
import workout.stub_services.AuthMock
import workout.utils.HttpSuite

import scala.concurrent.duration.DurationInt

object UserRoutesSuite extends HttpSuite {
  test("GET User") {
    val gen = for {
      u <- userGen
      b <- booleanGen
    } yield (u, b)

    forall(gen) { case (user, isAuthed) =>
      for {
        token <- AuthMock.tokens[IO].flatMap(_.create)
        _     <- if (isAuthed) RedisClient.put(token.value, user, 1.minute) else IO.unit
        req    = GET(uri"/user").putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token.value)))
        routes = new UserRoutes[IO].routes(usersMiddleware)
        res <-
          if (isAuthed)
            expectHttpBodyAndStatus(routes, req)(user, Status.Ok)
          else
            expectHttpStatus(routes, req)(Status.Forbidden)
      } yield res
    }
  }
}
