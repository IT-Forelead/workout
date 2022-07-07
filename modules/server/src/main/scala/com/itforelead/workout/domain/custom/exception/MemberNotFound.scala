package com.itforelead.workout.domain.custom.exception

import com.itforelead.workout.domain.types.UserId

import scala.util.control.NoStackTrace

case class MemberNotFound(userId: UserId) extends NoStackTrace
