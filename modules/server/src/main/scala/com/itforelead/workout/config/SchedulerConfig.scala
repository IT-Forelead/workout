package com.itforelead.workout.config

import java.time.LocalTime
import scala.concurrent.duration.FiniteDuration

case class SchedulerConfig(
    startTime: LocalTime,
    period: FiniteDuration,
  )
