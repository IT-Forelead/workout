package com.itforelead.workout.services

import cats.Monad
import cats.data.NonEmptyList
import cats.implicits._
import com.itforelead.workout.config.MonitoringConfig
import com.itforelead.workout.services.mailer.data.types.EmailAddress
import com.itforelead.workout.services.mailer.data.{ Content, Email, Mailer, Text }
import eu.timepit.refined.auto.autoUnwrap
import org.typelevel.log4cats.Logger

trait ErrorNotifier[F[_]] {
  def sendNotification(error: String): F[Unit]
}
object ErrorNotifier {
  def apply[F[_]: Monad: Logger](mailer: Mailer[F], config: MonitoringConfig): ErrorNotifier[F] =
    (error: String) =>
      for {
        _ <- Logger[F].info(s"Error handled via notifier appender: $error")
        email = Email(
          EmailAddress(config.username),
          "Error Notification",
          Content(Text(error).some),
          NonEmptyList.fromListUnsafe(config.recipients.map(s => EmailAddress(s))),
        )
        _ <- mailer.send(email)
      } yield ()
}
