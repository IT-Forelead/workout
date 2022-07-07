package com.itforelead.workout.domain.custom.exception

import com.itforelead.workout.domain.custom.refinements.Tel

import scala.util.control.NoStackTrace

case class ValidationCodeExpired(phone: Tel) extends NoStackTrace
