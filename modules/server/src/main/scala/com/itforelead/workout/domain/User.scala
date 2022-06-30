package com.itforelead.workout.domain

import com.itforelead.workout.domain.custom.refinements.{Password, Tel}
import com.itforelead.workout.domain.types._
import derevo.cats._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import io.circe.refined._
import eu.timepit.refined.cats._

@derive(decoder, encoder, show)
case class User(
  id: UserId,
  firstname: FirstName,
  lastname: LastName,
  phone: Tel
)

object User {

  @derive(decoder, encoder, show)
  case class CreateUser(
    firstname: FirstName,
    lastname: LastName,
    phone: Tel,
    password: Password
  )

  @derive(decoder, encoder, show)
  case class UserWithSettings(
    user: User,
    gymSettings: GymSettings
  )

  @derive(decoder, encoder)
  case class UserWithPassword(user: User, password: PasswordHash[SCrypt])
}
