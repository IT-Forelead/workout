package com.itforelead.workout.routes

import cats.implicits._
import cats.MonadThrow
import cats.effect.{Async, Sync}
import com.itforelead.workout.implicits._
import eu.timepit.refined.auto.autoUnwrap
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.custom.exception.MultipartDecodeError
import com.itforelead.workout.domain.custom.refinements.FileKey
import com.itforelead.workout.domain.{Member, User, ValidationPhone}
import com.itforelead.workout.services.{Members, Validations}
import com.itforelead.workout.services.s3._
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.multipart.Multipart
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger

final class UserValidationRoutes[F[_]: Async: Logger: JsonDecoder: MonadThrow](
  s3ClientStream: fs2.Stream[F, S3Client[F]],
  userValidation: Validations[F],
  members: Members[F]
)(implicit logger: Logger[F], F: Sync[F])
    extends Http4sDsl[F] {

  private[routes] val prefixPath = "/validation"

  private[this] val httpRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {
    case aR @ POST -> Root / "sent-code" as _ =>
      aR.req.decodeR[ValidationPhone] { validationPhone =>
        userValidation.sendValidationCode(validationPhone.phone) >> NoContent()
      }

    case aR @ POST -> Root / "code" as _ =>
      aR.req.decode[Multipart[F]] { multipart =>
        def uploadToS3(filename: String): fs2.Pipe[F, Byte, FileKey] = body =>
          for {
            s3Client <- s3ClientStream
            key      <- fs2.Stream.eval(genFileKey(filename))
            _        <- body.through(s3Client.putObject(filePath(key.value)))
          } yield key

        def createMember(form: CreateMember): F[Unit] =
          fs2.Stream
            .fromIterator(
              multipart.parts.fileParts.flatMap { p =>
                p.filename.filter(_.nonEmpty).map(f => (NonEmptyString.unsafeFrom(f), p.body)).toVector
              }.iterator,
              100
            )
            .flatMap { case (filename, body) =>
              body.through(uploadToS3(filename))
            }
            .evalMap { key =>
              userValidation.validatePhone(form, key)
            }
            .compile
            .drain

        (for {
          form <- multipart.parts.convert[CreateMember]
          response <-
            if (multipart.parts.isFilePartExists) {
              val streamRes = createMember(form).handleErrorWith { ex =>
                logger.error(ex)("Error occurred while upload product!") >>
                  F.raiseError[Unit](new Exception("Something went wrong!"))
              }
              Ok(streamRes)
            } else BadRequest("File part isn't defined")
        } yield response)
          .recoverWith {
            case error: MultipartDecodeError =>
              logger.error(s"Error occurred while parse multipart. Error: ${error.cause}") >>
                BadRequest(s"Bad form data. ${error.cause}")
            case error =>
              logger.error(error)("Error occurred creating product!") >>
                BadRequest("Error occurred creating product. Please try again!")
          }
      }

  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
