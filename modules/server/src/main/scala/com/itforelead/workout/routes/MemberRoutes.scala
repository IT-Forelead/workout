package com.itforelead.workout.routes

import cats.MonadThrow
import com.itforelead.workout.domain.User
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class MemberRoutes[F[_]: JsonDecoder: MonadThrow] extends Http4sDsl[F] {

  private[routes] val prefixPath = "/member"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of { case GET -> Root as member =>
    Ok(member)
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
