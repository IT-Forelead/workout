package com.itforelead.workout

import cats.MonadThrow
import cats.effect.{Async, Sync}
import cats.syntax.all._
import eu.timepit.refined.auto.autoUnwrap
import io.circe.{Decoder, Encoder}
import org.http4s.circe.{JsonDecoder, jsonEncoderOf, jsonOf, toMessageSyntax}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityDecoder, EntityEncoder, MediaType, Request, Response}
import com.itforelead.workout.domain.custom.refinements.{FileKey, FileName, FilePath}
import com.itforelead.workout.effects.GenUUID

package object routes {

  def getFileType(filename: FileName): String = filename.value.drop(filename.lastIndexOf(".") + 1)

  def filePath(fileId: String): FilePath = FilePath.unsafeFrom(fileId)

  def genFileKey[F[_]: Sync](orgFilename: FileName): F[FileKey] =
    GenUUID[F].make.map { uuid =>
      FileKey.unsafeFrom(uuid.toString + "." + getFileType(orgFilename))
    }

  implicit def deriveEntityEncoder[F[_]: Async, A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]

  implicit def deriveEntityDecoder[F[_]: Async, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

  def nameToContentType(filename: FileName): Option[`Content-Type`] =
    filename.lastIndexOf('.') match {
      case -1 => None
      case i  => MediaType.forExtension(filename.value.substring(i + 1)).map(`Content-Type`(_))
    }

  implicit class RefinedRequestDecoder[F[_]: JsonDecoder: MonadThrow](req: Request[F]) extends Http4sDsl[F] {

    def decodeR[A: Decoder](f: A => F[Response[F]]): F[Response[F]] =
      req.asJsonDecode[A].attempt.flatMap {
        case Left(e) =>
          Option(e.getCause) match {
            case Some(c) if c.getMessage.startsWith("Predicate") => BadRequest(c.getMessage)
            case _                                               => UnprocessableEntity()
          }
        case Right(a) => f(a)
      }

  }
}
