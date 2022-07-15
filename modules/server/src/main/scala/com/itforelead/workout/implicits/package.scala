package com.itforelead.workout

import cats.Monad
import cats.data.OptionT
import cats.effect.{Async, Sync}
import cats.implicits._
import com.itforelead.workout.domain.custom.exception.MultipartDecodeError
import com.itforelead.workout.domain.custom.refinements.{FilePath, Prefix}
import com.itforelead.workout.domain.custom.utils.MapConvert
import com.itforelead.workout.domain.custom.utils.MapConvert.ValidationResult
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Printer}
import org.http4s.MediaType
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.Part

import java.time.LocalDateTime

package object implicits {

  implicit class PartOps[F[_]: Async](parts: Vector[Part[F]]) {
    private def filterFileTypes(part: Part[F]): Boolean = part.filename.exists(_.trim.nonEmpty)

    def fileParts: Vector[Part[F]] = parts.filter(filterFileTypes)

    implicit class EnhancedPrefix(prefix: Prefix) {
      def /(id: String): FilePath = FilePath.unsafeFrom(s"$prefix/$id")
    }

    def fileParts(mediaType: MediaType): Vector[Part[F]] =
      parts.filter(_.headers.get[`Content-Type`].exists(_.mediaType == mediaType))

    def isFilePartExists: Boolean = parts.exists(filterFileTypes)

    def textParts: Vector[Part[F]] = parts.filterNot(filterFileTypes)

    def convert[A](implicit mapper: MapConvert[F, ValidationResult[A]], F: Sync[F]): F[A] =
      for {
        collectKV <- textParts.traverse { part =>
          part.bodyText.compile.foldMonoid.map(part.name.get -> _)
        }
        entity <- mapper.fromMap(collectKV.toMap)
        validEntity <- entity.fold(
          error => {
            F.raiseError[A](MultipartDecodeError(error.toList.mkString(" | ")))
          },
          success => success.pure[F]
        )
      } yield validEntity
  }

  implicit class CirceDecoderOps(s: String) {
    def as[A: Decoder]: A = decode[A](s).fold(throw _, json => json)
  }

  implicit class GenericTypeOps[A](obj: A) {
    private val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

    def toOptWhen(cond: => Boolean): Option[A] = if (cond) Some(obj) else None

    def toJson(implicit encoder: Encoder[A]): String = obj.asJson.printWith(printer)

    def toFormData[F[_]](implicit encoder: Encoder.AsObject[A]): Vector[Part[F]] =
      obj.asJsonObject.toVector
        .map { case k -> v =>
          k -> v.asString
        }
        .collect { case k -> Some(v) =>
          Part.formData[F](k, v)
        }
  }

  implicit class LocalDateTimeOps(ldt: LocalDateTime) {
    def endOfDay: LocalDateTime = ldt.withHour(23).withMinute(59).withSecond(59)
  }

  implicit class OptionTOps[F[_]: Monad](fa: F[Boolean]) {
    def asOptionT: OptionT[F, Unit] = {
      OptionT {
        fa.map {
          if (_) Some(()) else None
        }
      }
    }
  }
}
