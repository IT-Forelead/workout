package com.itforelead.workout.services.mailer.data

import com.itforelead.workout.services.mailer.data.Props.{
  DebugKey,
  SmtpAuthKey,
  SmtpConnectionTimeoutKey,
  SmtpHostKey,
  SmtpPortKey,
  SmtpStartTlsEnableKey,
  SmtpStartTlsRequiredKey,
  SmtpTimeoutKey,
  TransportProtocolKey,
  defaultProps,
}
import com.itforelead.workout.services.mailer.data.types.Protocol.Smtp
import com.itforelead.workout.services.mailer.data.types.{ Host, Port, Protocol }

import scala.concurrent.duration.{ DurationInt, FiniteDuration }

final case class Props(values: Map[String, String] = defaultProps) {
  def withSmtpAddress(host: Host, port: Port): Props =
    copy(values = values ++ Map(SmtpHostKey -> host.value, SmtpPortKey -> port.value.toString))

  def setConnectionTimeout(timeout: FiniteDuration): Props =
    copy(values = values ++ Map(SmtpConnectionTimeoutKey -> timeout.toMillis.toString))

  def setSmtpTimeout(timeout: FiniteDuration): Props =
    copy(values = values ++ Map(SmtpTimeoutKey -> timeout.toMillis.toString))

  def withTls(enable: Boolean = true, required: Boolean = false): Props =
    copy(values =
      values ++ Map(
        SmtpStartTlsEnableKey -> enable.toString,
        SmtpStartTlsRequiredKey -> required.toString,
      )
    )

  def setProtocol(protocol: Protocol): Props =
    copy(values = values ++ Map(TransportProtocolKey -> protocol.value))

  def withDebug(debug: Boolean = false): Props =
    copy(values = values ++ Map(DebugKey -> debug.toString))

  def withAuth(enable: Boolean = true): Props =
    copy(values = values ++ Map(SmtpAuthKey -> enable.toString))

  def set(key: String, value: String): Props =
    copy(values = values ++ Map(key -> value))
}

object Props {
  private[data] val DebugKey = "mail.debug"
  private[data] val SmtpConnectionTimeoutKey = "mail.smtp.connectiontimeout"
  private[data] val SmtpHostKey = "mail.smtp.host"
  private[data] val SmtpPortKey = "mail.smtp.port"
  private[data] val SmtpStartTlsEnableKey = "mail.smtp.starttls.enable"
  private[data] val SmtpSslProtocolKey = "mail.smtp.ssl.protocols"
  private[data] val SmtpStartTlsRequiredKey = "mail.smtp.starttls.required"
  private[data] val SmtpTimeoutKey = "mail.smtp.timeout"
  private[data] val TransportProtocolKey = "mail.transport.protocol"
  private[data] val SmtpAuthKey = "mail.smtp.auth"
  private[data] val defaultProps =
    Map(
      SmtpHostKey -> "localhost",
      SmtpPortKey -> "25",
      DebugKey -> "false",
      SmtpConnectionTimeoutKey -> 3.seconds.toMillis.toString,
      SmtpTimeoutKey -> 30.seconds.toMillis.toString,
      SmtpStartTlsEnableKey -> "true",
      SmtpSslProtocolKey -> "TLSv1.2",
      SmtpStartTlsRequiredKey -> "true",
      SmtpStartTlsRequiredKey -> "true",
      TransportProtocolKey -> Smtp.value,
      SmtpAuthKey -> "true",
    )
}
