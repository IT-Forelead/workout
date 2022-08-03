package com.itforelead.workout.syntax

import cats.effect.Async
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.parser.decode
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

trait CirceSyntax {
  implicit def circeSyntaxDecoderOps(s: String): DecoderOps = new DecoderOps(s)

  implicit def deriveEntityEncoder[F[_]: Async, A: Encoder]: EntityEncoder[F, A] =
    jsonEncoderOf[F, A]

  implicit def deriveEntityDecoder[F[_]: Async, A: Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

  implicit def mapEncoder[K: Encoder, V: Encoder]: Encoder[Map[K, V]] =
    (map: Map[K, V]) => Encoder[List[(K, V)]].apply(map.toList)

  implicit def mapDecoder[K: Decoder, V: Decoder]: Decoder[Map[K, V]] =
    (c: HCursor) => c.as[List[(K, V)]].map(_.toMap)
}

final class DecoderOps(private val s: String) {
  def as[A: Decoder]: A = decode[A](s).fold(throw _, json => json)
}
