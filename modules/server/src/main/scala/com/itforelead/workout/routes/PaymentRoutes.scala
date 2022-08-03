package com.itforelead.workout.routes

import cats.MonadThrow
import cats.implicits._
import com.itforelead.workout.domain.Payment.{CreatePayment, PaymentFilter, PaymentMemberId}
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.custom.exception.{CreatePaymentDailyTypeError, MemberNotFound}
import com.itforelead.workout.services.Payments
import com.itforelead.workout.implicits.http4SyntaxReqOps
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

    case ar @ POST -> Root / IntVar(page) as user =>
      ar.req.decodeR[PaymentFilter] { filter =>
        payments.getPaymentsWithTotal(user.id, filter, page).flatMap(Ok(_))
      }

    case ar @ POST -> Root / "member" as user =>
      ar.req.decodeR[PaymentMemberId] { form =>
        payments.getPaymentByMemberId(user.id, form.memberId).flatMap(Ok(_))
      }

    case ar @ POST -> Root as user =>
      ar.req
        .decodeR[CreatePayment] { createPayment =>
          payments.create(user.id, createPayment).flatMap(Created(_))
        }
        .recoverWith {
          case _: CreatePaymentDailyTypeError.type =>
            logger.error(s"This user has access to the GYM.") >>
              Response[F](status = MethodNotAllowed).withEntity("This user has access to the GYM.").pure[F]
          case _: MemberNotFound.type =>
            logger.error(s"This member not found from DB.") >>
              BadRequest("This member not found from DB.")
          case error =>
            logger.error(error)("Error occurred creating payment!") >>
              BadRequest("Error occurred creating payment. Please try again!")
        }
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
