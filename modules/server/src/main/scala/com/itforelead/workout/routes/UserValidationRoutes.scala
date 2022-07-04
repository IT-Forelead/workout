package com.itforelead.workout.routes

import cats.implicits._
import cats.MonadThrow
import com.itforelead.workout.config.AWSConfig
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.{Member, User, Validation}
import com.itforelead.workout.services.Validations
import com.itforelead.workout.services.s3.S3Client
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class UserValidationRoutes[F[_]: JsonDecoder: MonadThrow](
  s3ClientStream: fs2.Stream[F, S3Client[F]],
  userValidation: Validations[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/validation"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {
    case aR @ POST -> Root / "sent-code" as _ =>
      aR.req.decodeR[Validation] { validationPhone =>
        userValidation.sendValidationCode(validationPhone.phone) >> NoContent()
      }

    case aR @ POST -> Root / "code" as _ =>
      aR.req.decodeR[CreateMember] { validation =>
        userValidation.validatePhone(validation) >> Created()
      }
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
