package com.itforelead.workout.domain.custom.exception

import scala.util.control.NoStackTrace

final case class MultipartDecodeError(cause: String) extends NoStackTrace
