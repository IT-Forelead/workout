package com.itforelead.workout.routes

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.implicits._
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.User
import com.itforelead.workout.domain.custom.refinements.{FileName, FilePath}
import com.itforelead.workout.domain.types.UserId
import com.itforelead.workout.services.Members
import com.itforelead.workout.services.s3.S3Client
import org.http4s.headers.`Transfer-Encoding`
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger

final class MemberRoutes[F[_]: Async](members: Members[F], s3ClientStream: fs2.Stream[F, S3Client[F]])(implicit
  logger: Logger[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/member"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {
    case GET -> Root / UUIDVar(userId) / IntVar(page) as _ =>
      members.findByUserId(UserId(userId), page).flatMap(Ok(_))

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

    case GET -> Root / "image" / imageUrl as _ =>
      val imageStream =
        s3ClientStream.flatMap(_.downloadObject(FilePath.unsafeFrom(imageUrl)))
      Response(
        body = imageStream,
        headers = Headers(
          nameToContentType(imageUrl),
          `Transfer-Encoding`(TransferCoding.chunked.pure[NonEmptyList])
        )
      ).pure[F]

  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
