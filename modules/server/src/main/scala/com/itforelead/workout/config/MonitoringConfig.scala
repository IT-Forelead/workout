package com.itforelead.workout.config

import com.itforelead.workout.domain.custom.refinements.EmailAddress
import eu.timepit.refined.types.net.SystemPortNumber
import eu.timepit.refined.types.string.NonEmptyString

case class MonitoringConfig(
    host: NonEmptyString,
    port: SystemPortNumber,
    username: EmailAddress,
    password: NonEmptyString,
    recipients: List[EmailAddress],
  )
