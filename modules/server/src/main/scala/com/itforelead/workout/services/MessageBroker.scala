package com.itforelead.workout.services

import cats.effect.Async
import com.itforelead.workout.config.BrokerConfig
import com.itforelead.workout.domain.Message
import com.itforelead.workout.domain.broker.{BrokerMessage, Content, SMS}
import eu.timepit.refined.auto._
import org.http4s.Method.POST
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.{Accept, Authorization}
import org.http4s.{AuthScheme, BasicCredentials, Credentials, MediaType, Request}
import org.typelevel.log4cats.Logger

trait MessageBroker[F[_]] {
  def sendSMS(message: Message): F[Unit]
}

object MessageBroker {
  def apply[F[_]: Async: Logger](httpClint: Client[F], config: BrokerConfig): MessageBroker[F] =
    if (config.enabled)
      new MessageBrokerImpl[F](httpClint, config)
    else
      new MessageBrokerMock[F]

  private class MessageBrokerMock[F[_]: Logger] extends MessageBroker[F] {
    override def sendSMS(message: Message): F[Unit] =
      Logger[F].info(
        s"""Congratulation message sent to [$message.phone],
              message text [
                ${message.text}
              ] """
      )
  }

  private class MessageBrokerImpl[F[_]: Async](httpClient: Client[F], config: BrokerConfig)
      extends MessageBroker[F]
      with Http4sClientDsl[F] {
    private val ORIGINATOR: String = "3700"

    private def makeRequest(sms: BrokerMessage): Request[F] =
      POST(
        sms,
        config.apiURL,
        Authorization(Credentials.Token(AuthScheme.Basic, BasicCredentials(config.login, config.password.value).token)),
        Accept(MediaType.application.json)
      )

    private def makeSMS(message: Message) =
        BrokerMessage(
          message.phone,
          message.text,
          SMS(ORIGINATOR, Content(message.text))
        )

    override def sendSMS(message: Message): F[Unit] =
      httpClient.expect(makeRequest(makeSMS(message)))
  }

}
