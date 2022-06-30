package com.itforelead.workout.domain

import com.itforelead.workout.domain.custom.refinements.{FilePath, Tel, ValidationCode}
import com.itforelead.workout.domain.types._
import derevo.cats._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.refined._
import eu.timepit.refined.cats._

import java.time.LocalDate

@derive(decoder, encoder, show)
case class Member(
  id: MemberId,
  userId: UserId,
  firstname: FirstName,
  lastname: LastName,
  phone: Tel,
  birthday: LocalDate,
  image: FilePath
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
}
