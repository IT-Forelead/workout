package com.itforelead.workout.services

import cats.effect.Async
import cats.implicits.catsSyntaxApplicativeError
import com.itforelead.workout.config.MailerConfig
import com.itforelead.workout.services.mailer.data.types.{ EmailAddress, Host, Password, Port }
import com.itforelead.workout.services.mailer.data.{ Credentials, Email, Mailer, Props }
import org.typelevel.log4cats.Logger
import eu.timepit.refined.auto.autoUnwrap

trait EmailSender[F[_]] {
  val credentials: Credentials
  def send(email: Email): F[Unit]
}

object EmailSender {
  def apply[F[_]: Async: Logger](config: MailerConfig): EmailSender[F] = new EmailSender[F] {
    val credentials: Credentials = Credentials(
      EmailAddress(config.username),
      Password(config.password.value),
    )
    val props: Props = Props().withSmtpAddress(Host(config.host), Port(config.port))

    override def send(email: Email): F[Unit] =
      Mailer[F](props, credentials).send(email).onError { error =>
        Logger[F].error(error)("Error occurred while sending email.")
      }
  }
}
