package com.itforelead.workout.domain.custom.exception

import com.itforelead.workout.domain.types.UserId

import scala.util.control.NoStackTrace

case class UserIdIncorrect(uuid: UserId) extends NoStackTrace
