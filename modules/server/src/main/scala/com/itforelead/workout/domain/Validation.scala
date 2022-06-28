package com.itforelead.workout.domain

import com.itforelead.workout.domain.custom.refinements.{Tel, ValidationCode}
import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

@derive(decoder, encoder, show)
case class Validation(phone: Tel, code: ValidationCode)
