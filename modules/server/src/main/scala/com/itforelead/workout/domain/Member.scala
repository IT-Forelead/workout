package com.itforelead.workout.domain

import cats.effect.Sync
import cats.implicits._
import com.itforelead.workout.domain.custom.refinements.{FileKey, FilePath, Tel, ValidationCode}
import com.itforelead.workout.domain.custom.utils.MapConvert
import com.itforelead.workout.domain.custom.utils.MapConvert.ValidationResult
import com.itforelead.workout.domain.types._
import derevo.cats._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.refined._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString

import java.time.LocalDate
import java.util.UUID

@derive(decoder, encoder, show)
case class Member(
  id: MemberId,
  userId: UserId,
  firstname: FirstName,
  lastname: LastName,
  phone: Tel,
  birthday: LocalDate,
  image: FileKey
)

object Member {
  @derive(decoder, encoder, show)
  case class CreateMember(
    userId: UserId,
    firstname: FirstName,
    lastname: LastName,
    phone: Tel,
    birthday: LocalDate,
    image: FilePath,
    code: ValidationCode
  )

  @derive(decoder, encoder, show)
  case class MemberWithTotal(member: List[Member], total: Long)

  implicit def decodeMap[F[_]: Sync]: MapConvert[F, ValidationResult[CreateMember]] =
    (values: Map[String, String]) =>
      (
        values
          .get("userId")
          .map(id => UserId(UUID.fromString(id)).validNec)
          .getOrElse("Field [ userId ] isn't defined".invalidNec),
        values
          .get("firstname")
          .map(str => FirstName(NonEmptyString.unsafeFrom(str)).validNec)
          .getOrElse("Field [ firstname ] isn't defined".invalidNec),
        values
          .get("lastname")
          .map(str => LastName(NonEmptyString.unsafeFrom(str)).validNec)
          .getOrElse("Field [ lastname ] isn't defined".invalidNec),
        values
          .get("phone")
          .map(Tel.unsafeFrom(_).validNec)
          .getOrElse("Field [ phone ] isn't defined".invalidNec),
        values
          .get("birthday")
          .map(s => LocalDate.now.validNec)
          .getOrElse("Field [ birthday ] isn't defined".invalidNec),
        values
          .get("filePath")
          .map(FilePath.unsafeFrom(_).validNec)
          .getOrElse("Field [ file path ] isn't defined".invalidNec),
        values
          .get("code")
          .map(ValidationCode.unsafeFrom(_).validNec)
          .getOrElse("Field [ code ] isn't defined".invalidNec)
      ).mapN(CreateMember.apply).pure[F]

}
