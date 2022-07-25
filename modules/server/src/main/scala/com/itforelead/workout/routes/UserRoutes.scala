package com.itforelead.workout.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import com.itforelead.workout.domain.Role.ADMIN
import com.itforelead.workout.domain.UserSetting.UpdateSetting
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.User.{UserActivate, UserFilter}
import com.itforelead.workout.services.{Auth, UserSettings, Users}
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class UserRoutes[F[_]: JsonDecoder: MonadThrow](settings: UserSettings[F], users: Users[F]) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/user"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {

    case GET -> Root as user =>
      Ok(user)

    case ar @ POST -> Root / "clients" as user if user.role == ADMIN =>
      ar.req.decodeR[UserFilter] { filter =>
        users.getClients(filter).flatMap(Ok(_))
      }

    case GET -> Root / "settings" as user =>
      settings.settings(user.id).flatMap(Ok(_))

    case ar @ POST -> Root / "activate" as user if user.role == ADMIN =>
      ar.req.decodeR[UserActivate] { form =>
        users.userActivate(form).flatMap(Ok(_))
      }

    case ar @ PUT -> Root / "settings" as user =>
      ar.req.decodeR[UpdateSetting] { updateSettings =>
        settings.updateSettings(user.id, updateSettings).flatMap(Ok(_))
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
