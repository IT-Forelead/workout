package com.itforelead.workout.services.mailer.exception

case class InvalidAddress(cause: String) extends Throwable {
  override def getMessage: String = cause
}
