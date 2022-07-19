package com.itforelead.workout.routes

import cats.MonadThrow
import com.itforelead.workout.domain.Arrival.{ArrivalFilter, ArrivalMemberId, CreateArrival}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxFlatMapOps, toFlatMapOps}
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.custom.exception.MemberNotFound
import com.itforelead.workout.services.ArrivalService
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger

final class ArrivalRoutes[F[_]: JsonDecoder: MonadThrow](arrivalService: ArrivalService[F])(implicit
  logger: Logger[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/arrival"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {

    case GET -> Root as user =>
      arrivalService.get(user.id).flatMap(Ok(_))

    case ar @ POST -> Root / IntVar(page) as user =>
      ar.req.decodeR[ArrivalFilter] { filter =>
        arrivalService.getArrivalWithTotal(user.id, filter, page).flatMap(Ok(_))
      }

    case ar @ POST -> Root as user =>
      ar.req
        .decodeR[CreateArrival] { form =>
          arrivalService.create(user.id, form).flatMap(Created(_))
        }
        .recoverWith { case error: MemberNotFound.type =>
          logger.error(s"Member not found. Error: ${error}") >>
            NotFound("Member not found. Please try again")
        }

    case ar @ POST -> Root / "member" as user =>
      ar.req.decodeR[ArrivalMemberId] { form =>
        arrivalService.getArrivalByMemberId(user.id, form.memberId).flatMap(Ok(_))
      }

  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
