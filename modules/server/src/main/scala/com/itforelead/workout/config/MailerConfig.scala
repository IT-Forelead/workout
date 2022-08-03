package com.itforelead.workout.config

import ciris.Secret
import com.itforelead.workout.domain.custom.refinements.EmailAddress
import eu.timepit.refined.types.net.SystemPortNumber
import eu.timepit.refined.types.string.NonEmptyString

case class MailerConfig(
    host: NonEmptyString,
    port: SystemPortNumber,
    username: EmailAddress,
    password: Secret[NonEmptyString],
  )
