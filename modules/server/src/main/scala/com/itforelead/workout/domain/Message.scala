package com.itforelead.workout.domain

import com.itforelead.workout.domain.custom.refinements.Tel
import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import eu.timepit.refined.types.string.NonEmptyString

@derive(decoder, encoder, show)
case class Message(phone: Tel, text: NonEmptyString)
