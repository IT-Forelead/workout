package com.itforelead.workout.routes

import cats.MonadThrow
import cats.implicits.toFlatMapOps
import com.itforelead.workout.domain.Arrival.CreateArrival
import com.itforelead.workout.domain.Payment.CreatePayment
import com.itforelead.workout.domain.User
import com.itforelead.workout.services.ArrivalService
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

final class ArrivalRoutes[F[_]: JsonDecoder: MonadThrow](arrivalService: ArrivalService[F]) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/arrival"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {

    case GET -> Root as user =>
      arrivalService.get(user.id).flatMap(Ok(_))

    case ar @ POST -> Root as user =>
      ar.req.decodeR[CreateArrival] { form =>
        arrivalService.create(form).flatMap(Created(_))
      }

  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
