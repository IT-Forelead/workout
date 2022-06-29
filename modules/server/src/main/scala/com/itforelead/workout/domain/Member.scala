package com.itforelead.workout.domain

import com.itforelead.workout.domain.custom.refinements.{FilePath, Password, Tel, ValidationCode}
import com.itforelead.workout.domain.types._
import derevo.cats._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import io.circe.refined._
import eu.timepit.refined.cats._

import java.time.LocalDate

@derive(decoder, encoder, show)
case class Member(
  id: MemberId,
  gymId: GymId,
  firstname: UserName,
  lastname: UserName,
  phone: Tel,
  birthday: LocalDate,
  userPicture: FilePath
)

object Member {

  @derive(decoder, encoder, show)
  case class CreateMember(
    gymId: GymId,
    firstname: UserName,
    lastname: UserName,
    phone: Tel,
    birthday: LocalDate,
    userPicture: FilePath,
    code: ValidationCode
  )
}
