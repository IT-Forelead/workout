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

import java.util.UUID

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

  @derive(decoder, encoder)
  case class UserWithPassword(user: User, password: PasswordHash[SCrypt])

  val userId: UserId = UserId(UUID.fromString("76c2c44c-8fbf-4184-9199-19303a042fa0"))

}
