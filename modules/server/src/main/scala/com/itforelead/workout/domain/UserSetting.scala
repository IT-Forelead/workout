package com.itforelead.workout.domain

import com.itforelead.workout.domain.types._
import derevo.cats._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import squants.Money
import io.circe.refined._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString

@derive(decoder, encoder, show)
case class UserSetting(
  userId: UserId,
  gymName: GymName,
  dailyPrice: Money,
  monthlyPrice: Money
)

object UserSetting {

  @derive(decoder, encoder, show)
  case class UpdateSetting(
    gymName: GymName,
    dailyPrice: Money,
    monthlyPrice: Money
  )
}
