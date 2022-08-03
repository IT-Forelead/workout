package com.itforelead.workout.syntax
import java.time.LocalDateTime

trait JavaTimeSyntax {
  implicit def javaTimeSyntaxLocalDateTimeOps(ldt: LocalDateTime): LocalDateTimeOps = new LocalDateTimeOps(ldt)
}

final class LocalDateTimeOps(private val ldt: LocalDateTime) {
  def endOfDay: LocalDateTime = ldt.withHour(23).withMinute(59).withSecond(59)
}
