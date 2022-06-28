package com.itforelead.workout.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import com.itforelead.workout.domain.Payment.CreatePayment
import com.itforelead.workout.domain.User
import com.itforelead.workout.services.Payments
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class PaymentRoutes[F[_]: JsonDecoder: MonadThrow](payments: Payments[F]) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/payment"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {

    case GET -> Root as user =>
      payments.payments.flatMap(Ok(_))

    case ar @ POST -> Root as user =>
      ar.req.decodeR[CreatePayment] { createPayment =>
        payments.create(createPayment).flatMap(Created(_))
      }

  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
