package com.itforelead.workout.routes

import cats.implicits._
import cats.MonadThrow
import com.itforelead.workout.domain.{User, Validation}
import com.itforelead.workout.domain.custom.refinements.Tel
import com.itforelead.workout.services.UserValidation
import io.circe.refined.refinedDecoder
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class UserValidationRoutes[F[_]: JsonDecoder: MonadThrow](userValidation: UserValidation[F])
    extends Http4sDsl[F] {

  private[routes] val prefixPath = "/validation"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {
    case aR @ POST -> Root as _ =>
      aR.req.decodeR[Tel] { phone =>
        userValidation.sendValidationCode(phone) >> NoContent()
      }

    case aR @ POST -> Root as _ =>
      aR.req.decodeR[Validation] { validation =>
        userValidation.validatePhone(validation) >> Ok()
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
