package workout.http.routes

import cats.effect.IO.delay
import cats.effect.{IO, Sync}
import cats.implicits._
import com.itforelead.workout.domain.Role.ADMIN
import com.itforelead.workout.domain.{User, UserSetting}
import com.itforelead.workout.domain.UserSetting.UpdateSetting
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.{UserRoutes, deriveEntityEncoder}
import io.circe.generic.encoding.ReprAsObjectEncoder.deriveReprAsObjectEncoder
import org.http4s.Method.{GET, PUT}
import org.http4s.client.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{AuthScheme, Credentials, Status}
import workout.stub_services.{AuthMock, UserSettingsMock, UserSettingsStub, UsersStub}
import workout.utils.Generators._
import workout.utils.HttpSuite

import scala.concurrent.duration.DurationInt

object UserRoutesSuite extends HttpSuite {
  private def settings[F[_]: Sync: GenUUID](setting: UserSetting): UserSettingsMock[F] = new UserSettingsMock[F] {
    override def settings(userId: UserId): F[UserSetting]                                = Sync[F].delay(setting)
    override def updateSettings(userId: UserId, settings: UpdateSetting): F[UserSetting] = Sync[F].delay(setting)
  }

  private def users[F[_]: Sync: GenUUID](user: User): UsersStub[F] = new UsersStub[F] {
    override def getClients: F[List[User]] = Sync[F].delay(List(user))
  }

  test("PUT User Settings") {
    val gen = for {
      u <- userGen()
      s <- userSettingGen()
    } yield (u, s)

    forall(gen) { case (user, setting) =>
      for {
        token <- authToken(user)
        req    = PUT(setting, uri"/user/settings").putHeaders(token)
        routes = new UserRoutes[IO](settings(setting), users(user)).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(setting, Status.Ok)
      } yield res
    }
  }

  test("GET User Settings By Id") {
    val gen = for {
      u <- userGen()
      s <- userSettingGen()
    } yield (u, s)

    forall(gen) { case (user, setting) =>
      for {
        token <- authToken(user)
        req    = GET(uri"/user/settings").putHeaders(token)
        routes = new UserRoutes[IO](settings(setting), users(user)).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(setting, Status.Ok)
      } yield res
    }
  }

  test("GET User") {
    val gen = for {
      u <- userGen()
      b <- booleanGen
    } yield (u, b)

    forall(gen) { case (user, isAuthed) =>
      for {
        token <- AuthMock.tokens[IO].flatMap(_.create)
        _     <- if (isAuthed) RedisClient.put(token.value, user, 1.minute) else IO.unit
        req    = GET(uri"/user").putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token.value)))
        routes = new UserRoutes[IO](new UserSettingsStub[IO] {}, users(user)).routes(usersMiddleware)
        res <-
          if (isAuthed)
            expectHttpBodyAndStatus(routes, req)(user, Status.Ok)
          else
            expectHttpStatus(routes, req)(Status.Forbidden)
      } yield res
    }
  }

  test("GET Clients") {
    val gen = for {
      u <- userGen(ADMIN)
      c <- userGen()
      s <- userSettingGen()
    } yield (u, c, s)

    forall(gen) { case (user, client, setting) =>
      for {
        token <- authToken(user)
        req    = GET(uri"/user/clients").putHeaders(token)
        routes = new UserRoutes[IO](settings(setting), users(client)).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(List(client), Status.Ok)
      } yield res
    }
  }
}
