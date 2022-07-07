package com.itforelead.workout.domain.custom.exception

import com.itforelead.workout.domain.custom.refinements.ValidationCode

import scala.util.control.NoStackTrace

case class ValidationCodeError(code: ValidationCode) extends NoStackTrace
