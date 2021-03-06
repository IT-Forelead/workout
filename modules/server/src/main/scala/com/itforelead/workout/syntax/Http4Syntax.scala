package com.itforelead.workout.syntax

import cats.MonadThrow
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all._
import com.itforelead.workout.domain.custom.exception.MultipartDecodeError
import com.itforelead.workout.domain.custom.utils.MapConvert
import com.itforelead.workout.domain.custom.utils.MapConvert.ValidationResult
import com.itforelead.workout.implicits.circeSyntaxDecoderOps
import com.itforelead.workout.services.redis.RedisClient
import io.circe.Decoder
import org.http4s.circe.{ JsonDecoder, toMessageSyntax }
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.multipart.Part
import org.http4s.{ MediaType, Request, Response }

import scala.concurrent.duration.DurationInt

trait Http4Syntax {
  implicit def http4SyntaxReqOps[F[_]: JsonDecoder: MonadThrow](
      request: Request[F]
    ): RequestOps[F] =
    new RequestOps(request)
  implicit def http4SyntaxPartOps[F[_]](parts: Vector[Part[F]]): PartOps[F] =
    new PartOps(parts)
}

final class RequestOps[F[_]: JsonDecoder: MonadThrow](private val request: Request[F])
    extends Http4sDsl[F] {
  def decodeR[A: Decoder](f: A => F[Response[F]]): F[Response[F]] =
    request.asJsonDecode[A].attempt.flatMap {
      case Left(e) =>
        Option(e.getCause) match {
          case Some(c) if c.getMessage.startsWith("Predicate") => BadRequest(c.getMessage)
          case _ => UnprocessableEntity()
        }
      case Right(a) => f(a)
    }

  def checkIp(
      allowedAttempt: Int = 3
    )(
      response: => F[Response[F]]
    )(implicit
      redis: RedisClient[F]
    ): F[Response[F]] =
    request.remoteAddr match {
      case Some(ipAddress) =>
        OptionT(redis.get(ipAddress.toString))
          .map(_.as[Int])
          .cataF(
            redis.put(ipAddress.toString, 1, 1.days) >> response,
            attempt =>
              if (attempt < allowedAttempt)
                redis.put(ipAddress.toString, attempt + 1, 1.days) >> response
              else
                TooManyRequests("Requests have exceeded the specified limit"),
          )
      case None => Forbidden("Request without IP-Address")
    }
}

final class PartOps[F[_]](private val parts: Vector[Part[F]]) {
  private def filterFileTypes(part: Part[F]): Boolean = part.filename.exists(_.trim.nonEmpty)

  def fileParts: Vector[Part[F]] = parts.filter(filterFileTypes)

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
        error => F.raiseError[A](MultipartDecodeError(error.toList.mkString(" | "))),
        success => success.pure[F],
      )
    } yield validEntity
}
