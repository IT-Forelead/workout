package com.itforelead.workout.domain

import com.itforelead.workout.domain.custom.refinements.{ Password, Tel, ValidationCode }
import com.itforelead.workout.domain.types._
import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.SCrypt
import io.circe.refined._
import eu.timepit.refined.cats._
import squants.Money

import java.util.UUID

@derive(decoder, encoder, show)
case class User(
    id: UserId,
    firstname: FirstName,
    lastname: LastName,
    phone: Tel,
    role: Role,
    activate: Boolean,
  )

object User {
  @derive(decoder, encoder, show)
  case class CreateClient(
      firstname: FirstName,
      lastname: LastName,
      gymName: GymName,
      dailyPrice: Money,
      monthlyPrice: Money,
      phone: Tel,
      code: ValidationCode,
      password: Password,
    )

  @derive(decoder, encoder)
  case class UserWithPassword(user: User, password: PasswordHash[SCrypt])

  @derive(decoder, encoder, show)
  case class UserWithSetting(
      user: User,
      setting: UserSetting,
    )

  @derive(decoder, encoder, show)
  case class UserWithTotal(
      user: List[UserWithSetting],
      total: Long,
    )

  @derive(decoder, encoder, show)
  case class UserFilter(
      typeBy: Option[UserFilterBy] = None,
      sortBy: Boolean,
    )

  @derive(decoder, encoder, show)
  case class UserActivate(userId: UserId)

  val userId: UserId = UserId(UUID.fromString("76c2c44c-8fbf-4184-9199-19303a042fa0"))
}
