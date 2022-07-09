package com.itforelead.workout.routes

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.effect.kernel.Async
import cats.implicits._
import com.itforelead.workout.domain.Member.CreateMember
import com.itforelead.workout.domain.custom.exception._
import com.itforelead.workout.domain.custom.refinements.{FileKey, FileName, FilePath}
import com.itforelead.workout.domain.{Member, User, Validation}
import com.itforelead.workout.implicits.PartOps
import com.itforelead.workout.services.{Auth, Members}
import com.itforelead.workout.services.s3.S3Client
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Transfer-Encoding`
import org.http4s.multipart.Multipart
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger

object MemberRoutes {
  val prefixPath = "/client"

  def apply[F[_]: Async](
    s3Client: S3Client[F],
    member: Members[F]
  )(implicit logger: Logger[F], F: Sync[F], authService: Auth[F]): MemberRoutes[F] = new MemberRoutes(
    member,
    s3Client
  )
}

final class MemberRoutes[F[_]: Async](members: Members[F], s3Client: S3Client[F])(implicit
  logger: Logger[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/member"

  private def uploadToS3(filename: FileName): fs2.Pipe[F, Byte, FileKey] = body =>
    for {
      key <- fs2.Stream.eval(genFileKey(filename))
      _   <- body.through(s3Client.putObject(filePath(key.value)))
    } yield key

  private[this] val privateRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {

    case GET -> Root / IntVar(page) as user =>
      members.findByUserId(user.id, page).flatMap(Ok(_))

    case aR @ POST -> Root / "sent-code" as user =>
      aR.req.decodeR[Validation] { validationPhone =>
        members.sendValidationCode(user.id, validationPhone.phone).flatMap(Ok(_))
      }

    case aR @ PUT -> Root as user =>
      aR.req.decode[Multipart[F]] { multipart =>
        def uploadFile: F[List[FileKey]] =
          fs2.Stream
            .fromIterator(
              multipart.parts.fileParts.flatMap { p =>
                p.filename.filter(_.nonEmpty).map(f => (FileName.unsafeFrom(f), p.body)).toVector
              }.iterator,
              100
            )
            .flatMap { case (filename, body) =>
              body.through(uploadToS3(filename))
            }
            .compile
            .toList

        def createMember(form: CreateMember): F[Unit] = {
          uploadFile.flatMap { list =>
            println(s"LIST LENGTH: ${list.length}")
            list.traverse_ { key =>
              members.validateAndCreate(user.id, form, key)
            }
          }
        }

        (for {
          form <- multipart.parts.convert[CreateMember]
          response <-
            if (multipart.parts.isFilePartExists) {
              createMember(form).flatMap(Created(_))
            } else BadRequest("File part isn't defined")
        } yield response)
          .recoverWith {
            case codeExpiredError: ValidationCodeExpired =>
              logger.error(s"Validation code expired. Error: ${codeExpiredError.phone.value}") >>
                NotAcceptable("Validation code expired. Please try again")
            case phoneInUseError: PhoneInUse =>
              logger.error(s"Phone is already in use. Error: ${phoneInUseError.phone.value}") >>
                NotAcceptable("Phone is already in use. Please try again with other phone number")
            case calCodeError: ValidationCodeIncorrect =>
              logger.error(s"Validation code is wrong. Error: ${calCodeError.code.value}") >>
                NotAcceptable("Validation code is wrong. Please try again")
            case error: MultipartDecodeError =>
              logger.error(s"Error occurred while parse multipart. Error: ${error.cause}") >>
                BadRequest(s"Bad form data. ${error.cause}")
            case error =>
              logger.error(error)("Error occurred creating member!") >>
                BadRequest("Error occurred creating member. Please try again!")
          }
      }
  }

  private val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "image" / imageUrl =>
    val imageStream =
      s3Client.downloadObject(FilePath.unsafeFrom(imageUrl))
    Response(
      body = imageStream,
      headers = Headers(
        nameToContentType(FileName.unsafeFrom(imageUrl)),
        `Transfer-Encoding`(TransferCoding.chunked.pure[NonEmptyList])
      )
    ).pure[F]

  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = Router(
    prefixPath -> (publicRoutes <+> authMiddleware(privateRoutes))
  )

}
