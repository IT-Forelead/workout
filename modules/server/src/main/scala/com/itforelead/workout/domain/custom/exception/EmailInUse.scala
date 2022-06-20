package com.itforelead.workout.domain.custom.exception

import com.itforelead.workout.domain.custom.refinements.EmailAddress
import scala.util.control.NoStackTrace

case class EmailInUse(email: EmailAddress) extends NoStackTrace
