package com.itforelead.workout.services.mailer.exception

sealed trait DeliverFailure extends Throwable {
  def cause: String
  override def getMessage: String = cause
}
object DeliverFailure {
  case class AuthenticationFailed(cause: String) extends DeliverFailure
}
