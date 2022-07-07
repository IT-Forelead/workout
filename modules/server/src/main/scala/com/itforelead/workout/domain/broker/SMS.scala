package com.itforelead.workout.domain.broker

import derevo.cats.show
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

@derive(decoder, encoder, show)
case class SMS(
  originator: String,
  content: Content
)
