package com.itforelead.workout.config

import ciris.Secret
import eu.timepit.refined.types.all.NonEmptyString
import org.http4s.Uri

case class BrokerConfig(
    apiURL: Uri,
    login: NonEmptyString,
    password: Secret[NonEmptyString],
    enabled: Boolean = false,
  )
