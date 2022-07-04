package com.itforelead.workout.routes

import cats.effect.kernel.Async
import cats.implicits._
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.Messages
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger

final class MessageRoutes[F[_]: Async](messages: Messages[F])(implicit logger: Logger[F]) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/message"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of { case GET -> Root as user =>
    messages.get(user.id).flatMap(Ok(_))
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
