package com.itforelead.workout.domain

import com.itforelead.workout.domain.custom.refinements.Tel
import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.refined._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString

@derive(decoder, encoder, show)
case class Validation(phone: Tel)

object Validation {
  @derive(decoder, encoder, show)
  case class ValidationMessage(phone: Tel, text: NonEmptyString)
}
