package com.itforelead.workout.domain.circe
import io.circe.{Decoder, Encoder, HCursor}

trait MapCodec {
  implicit def mapEncoder[K: Encoder, V: Encoder]: Encoder[Map[K, V]] =
    (map: Map[K, V]) => Encoder[List[(K, V)]].apply(map.toList)

  implicit def mapDecoder[K: Decoder, V: Decoder]: Decoder[Map[K, V]] =
    (c: HCursor) => c.as[List[(K, V)]].map(_.toMap)
}
