package com.itforelead.workout.domain

import cats.effect.Sync
import cats.implicits._
import com.itforelead.workout.domain.custom.utils.MapConvert
import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import com.itforelead.workout.domain.custom.refinements.{EmailAddress, Password, Tel}
import com.itforelead.workout.domain.custom.utils.MapConvert.ValidationResult
import io.circe.refined._
import eu.timepit.refined.cats._

@derive(decoder, encoder, show)
case class Credentials(phone: Tel, password: Password)
object Credentials {

  implicit def decodeMap[F[_]: Sync]: MapConvert[F, ValidationResult[Credentials]] =
    (values: Map[String, String]) =>
      (
        values
          .get("phone")
          .map(Tel.unsafeFrom(_).validNec)
          .getOrElse("Field [ phone ] isn't defined".invalidNec),
        values
          .get("password")
          .map(Password.unsafeFrom(_).validNec)
          .getOrElse("Field [ password ] isn't defined".invalidNec)
      ).mapN(Credentials.apply).pure[F]
}
