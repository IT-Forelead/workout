package com.itforelead.workout

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.itforelead.workout.config.ConfigLoader
import com.itforelead.workout.services.ErrorNotifier
import com.itforelead.workout.services.mailer.data.types.{ EmailAddress, Host, Password, Port }
import com.itforelead.workout.services.mailer.data.{ Credentials, Mailer, Props }
import eu.timepit.refined.auto.autoUnwrap
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext.Implicits
import scala.util.Try

class NotifierAppender[A] extends AppenderBase[A] {
  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val notifier: IO[ErrorNotifier[IO]] =
    for {
      conf <- ConfigLoader.monitoringConfig[IO]
      credentials = Credentials(
        EmailAddress(conf.username),
        Password(conf.password),
      )
      props = Props().withSmtpAddress(Host(conf.host.value), Port(conf.port.value))
      mailer = Mailer[IO](props, credentials)
      notifier = ErrorNotifier[IO](mailer, conf)
    } yield notifier
  val (appender, canceller) = notifier.unsafeToFutureCancelable()

  override def start(): Unit =
    super.start()

  override def stop(): Unit = {
    canceller()
    super.stop()
  }

  override def append(eventObject: A): Unit =
    Try {
      eventObject match {
        case loggingEvent: ILoggingEvent =>
          val msg = loggingEvent.getFormattedMessage
          val className = loggingEvent.getLoggerName.split('.').last
          val throwableProxy = loggingEvent.getThrowableProxy
          if (throwableProxy != null)
            s"$className | $msg | ${throwableProxy.getMessage}"
          else
            s"$className | $msg"

        case _ =>
          eventObject.toString
      }
    }.fold(
      error => sendNotification(s"${eventObject.toString} | [logging-error] ${error.toString}"),
      sendNotification,
    )

  private def sendNotification(errorText: String): Unit =
    appender.foreach(_.sendNotification(errorText).unsafeRunSync())(Implicits.global)
}
