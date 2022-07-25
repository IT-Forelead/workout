package workout.http.routes

import cats.effect.IO.delay
import cats.effect.{IO, Sync}
import cats.implicits._
import com.itforelead.workout.domain.User.{UserActivate, UserFilter, UserWithSetting}
import com.itforelead.workout.domain.Role.ADMIN
import com.itforelead.workout.domain.{User, UserSetting}
import com.itforelead.workout.domain.UserSetting.UpdateSetting
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.routes.{UserRoutes, deriveEntityEncoder}
import io.circe.generic.encoding.ReprAsObjectEncoder.deriveReprAsObjectEncoder
import org.http4s.Method.{GET, POST, PUT}
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

  private def users[F[_]: Sync: GenUUID](user: User, setting: UserSetting): UsersStub[F] = new UsersStub[F] {
    override def getClients(filter: UserFilter): F[List[UserWithSetting]] =
      Sync[F].delay(List(UserWithSetting(user, setting)))
    override def userActivate(userActivate: UserActivate): F[User] = Sync[F].delay(user)
  }

  test("PUT User Settings") {
    val gen = for {
      u <- userGen()
      s <- userSettingGen()
      us <- updateSettingGen
    } yield (u, s, us)

    forall(gen) { case (user, setting, updateSetting) =>
      for {
        token <- authToken(user)
        req    = PUT(updateSetting, uri"/user/settings").putHeaders(token)
        routes = new UserRoutes[IO](settings(setting), users(user, setting)).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(setting, Status.Ok)
      } yield res
    }
  }

  test("POST User activate") {
    val gen = for {
      u <- userGen(ADMIN)
      us <- userSettingGen()
      ua <- userActivateGen
    } yield (u, us, ua)

    forall(gen) { case (user, setting, userActivate) =>
      for {
        token <- authToken(user)
        req    = POST(userActivate, uri"/user/activate").putHeaders(token)
        routes = new UserRoutes[IO](settings(setting), users(user, setting)).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(user, Status.Ok)
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
        routes = new UserRoutes[IO](settings(setting), users(user, setting)).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(setting, Status.Ok)
      } yield res
    }
  }

  test("GET User") {
    val gen = for {
      u <- userGen()
      s <- userSettingGen()
      b <- booleanGen
    } yield (u, s, b)

    forall(gen) { case (user, setting, isAuthed) =>
      for {
        token <- AuthMock.tokens[IO].flatMap(_.create)
        _     <- if (isAuthed) RedisClient.put(token.value, user, 1.minute) else IO.unit
        req    = GET(uri"/user").putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token.value)))
        routes = new UserRoutes[IO](new UserSettingsStub[IO] {}, users(user, setting)).routes(usersMiddleware)
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
      f <- userFilterGen
    } yield (u, c, s, f)

    forall(gen) { case (user, client, setting, filter) =>
      for {
        token <- authToken(user)
        req    = POST(filter, uri"/user/clients").putHeaders(token)
        routes = new UserRoutes[IO](settings(setting), users(client, setting)).routes(usersMiddleware)
        res <- expectHttpBodyAndStatus(routes, req)(List(UserWithSetting(client, setting)), Status.Ok)
      } yield res
    }
  }
}
