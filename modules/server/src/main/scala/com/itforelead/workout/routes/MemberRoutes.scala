package com.itforelead.workout.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.domain.User
import com.itforelead.workout.services.Members
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class MemberRoutes[F[_]: JsonDecoder: MonadThrow](members: Members[F]) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/member"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of { case GET -> Root / UUIDVar(userId) as _ =>
    members.findByUserId(UserId(userId)).flatMap(Ok(_))

  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
