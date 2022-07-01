package com.itforelead.workout.routes

import cats.effect.kernel.Async
import cats.implicits._
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.Members
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger

final class MemberRoutes[F[_]: Async](members: Members[F])(implicit logger: Logger[F]) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/member"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of { case GET -> Root / UUIDVar(userId) as _ =>
    members.findByUserId(UserId(userId)).flatMap(Ok(_))

//    case aR @ POST -> Root as _ =>
//      (for {
//        form   <- aR.req.as[CreateMember]
//        member <- members.create(form)
//        res    <- Ok(member)
//      } yield res)
//        .handleErrorWith { err =>
//          logger.error(s"Error occurred while add member. Error: $err") >>
//            BadRequest("Error occurred while add member. Please try again!")
//        }

  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
