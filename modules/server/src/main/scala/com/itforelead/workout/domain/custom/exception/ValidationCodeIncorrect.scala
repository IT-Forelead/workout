package com.itforelead.workout.domain.custom.exception

import com.itforelead.workout.domain.custom.refinements.ValidationCode

import scala.util.control.NoStackTrace

case class ValidationCodeIncorrect(code: ValidationCode) extends NoStackTrace
