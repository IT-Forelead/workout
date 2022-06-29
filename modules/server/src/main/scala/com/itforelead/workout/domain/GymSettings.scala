package com.itforelead.workout.domain

import com.itforelead.workout.domain.types.{GymName, UserId}
import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import squants.Money

@derive(decoder, encoder, show)
case class GymSettings(
  userId: UserId,
  name: GymName,
  dailyPrice: Money,
  monthlyPrice: Money
)
