package com.itforelead.workout.syntax

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Printer}
import org.http4s.multipart.Part

trait GenericSyntax {
  implicit def genericSyntaxGenericTypeOps[A](obj: A): GenericTypeOps[A] = new GenericTypeOps[A](obj)
}

final class GenericTypeOps[A](obj: A) {
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
