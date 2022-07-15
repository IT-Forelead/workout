package com.itforelead.workout.security

import cats.MonadThrow
import cats.data.{Kleisli, OptionT}
import cats.syntax.all._
import dev.profunktor.auth.AuthHeaders
import dev.profunktor.auth.jwt._
import org.http4s.{AuthedRoutes, Request}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import pdi.jwt._
import pdi.jwt.exceptions.JwtException

object JwtAuthMiddleware {
  def apply[F[_]: MonadThrow, A](
    jwtAuth: JwtAuth,
    authenticate: JwtToken => JwtClaim => F[Option[(A, Option[JwtToken])]]
  )(routes: AuthedRoutes[A, F]): AuthMiddleware[F, A] = {
    val dsl = new Http4sDsl[F] {}; import dsl._

    val onFailure: AuthedRoutes[String, F] =
      Kleisli(req => OptionT.liftF(Forbidden(req.context)))

    val authUser: Kleisli[F, Request[F], Either[String, A]] =
      Kleisli { request =>
        AuthHeaders.getBearerToken(request).fold("Bearer token not found".asLeft[A].pure[F]) { token =>
          OptionT(jwtDecode[F](token, jwtAuth).flatMap(authenticate(token)))
            .map(a => a.fold("not found".asLeft[A])(_.asRight[String]))
            .recover { case _: JwtException =>
              "Invalid access token".asLeft[A]
            }
        }
      }
    val authUser1: Kleisli[F, Request[F], Either[String, A]] =
      Kleisli { request =>
        AuthHeaders.getBearerToken(request).fold("Bearer token not found".asLeft[A].pure[F]) { token =>
          OptionT(jwtDecode[F](token, jwtAuth).flatMap(authenticate(token)))
            .map(a => a.fold("not found".asLeft[A])(_.asRight[String]))
            .recover { case _: JwtException =>
              "Invalid access token".asLeft[A]
            }
        }
      }

    val authMiddleware = AuthMiddleware[F, String, A](authUser, onFailure)
    authMiddleware(routes).andThen(res => OptionT.liftF[F, Response[F]](res.withHeaders()))
  }
}
