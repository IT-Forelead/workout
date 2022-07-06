package com.itforelead.workout.routes

import cats.MonadThrow
import cats.implicits._
import com.itforelead.workout.domain.Payment.CreatePayment
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.custom.exception.MemberCurrentActiveTime
import com.itforelead.workout.services.Payments
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger

final class PaymentRoutes[F[_]: JsonDecoder: MonadThrow](payments: Payments[F])(implicit logger: Logger[F])
    extends Http4sDsl[F] {

  private[routes] val prefixPath = "/payment"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {

    case GET -> Root as user =>
      payments.payments(user.id).flatMap(Ok(_))

    case ar @ POST -> Root as user =>
      ar.req
        .decodeR[CreatePayment] { createPayment =>
          payments.create(createPayment.copy(userId = user.id)).flatMap(Created(_))
        }
        .recoverWith {
          case userAccessError: MemberCurrentActiveTime.type =>
            logger.error(s"This user has access to the GYM.") >>
              Response[F](status = MethodNotAllowed).withEntity("This user has access to the GYM.").pure[F]
          case error =>
            logger.error(error)("Error occurred creating payment!") >>
              BadRequest("Error occurred creating payment. Please try again!")
        }
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
