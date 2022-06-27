package com.itforelead.workout.domain

import com.itforelead.workout.domain.custom.refinements.{FilePath, Password, Tel}
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
case class User(
  id: UserId,
  fullname: UserName,
  phoneNumber: Tel,
  birthday: LocalDate,
  userPicture: FilePath,
  role: Role
)

object User {

  @derive(decoder, encoder, show)
  case class CreateUser(
    fullname: UserName,
    phoneNumber: Tel,
    birthday: LocalDate,
    userPicture: FilePath,
    password: Password
  )

  @derive(decoder, encoder)
  case class UserWithPassword(user: User, password: PasswordHash[SCrypt])
}
