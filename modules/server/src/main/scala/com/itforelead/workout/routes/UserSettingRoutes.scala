package com.itforelead.workout.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import com.itforelead.workout.domain.Payment.CreatePayment
import com.itforelead.workout.domain.{User, UserSetting}
import com.itforelead.workout.services.{Payments, UserSettings}
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class UserSettingRoutes[F[_]: JsonDecoder: MonadThrow](settings: UserSettings[F]) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/user-settings"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {

    case GET -> Root as user =>
      settings.settings(user.id).flatMap(Ok(_))

    case ar @ PUT -> Root as _ =>
      ar.req.decodeR[UserSetting] { updateSettings =>
        settings.updateSettings(updateSettings).flatMap(Ok(_))
      }

  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
