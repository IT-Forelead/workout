package com.itforelead.workout.modules

import cats.effect.Resource
import cats.effect.kernel.Async
import com.itforelead.workout.config.BrokerConfig
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services._
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import skunk.Session

object Services {
  def apply[F[_]: Async: GenUUID: Logger](
    brokerConfig: BrokerConfig,
    httpClient: Client[F]
  )(implicit session: Resource[F, Session[F]]): Services[F] = {

    new Services[F](
      users = Users[F],
      payments = Payments[F],
      messageBroker = MessageBroker[F](httpClient, brokerConfig)
    )
  }
}

final class Services[F[_]] private (
  val users: Users[F],
  val payments: Payments[F],
  val messageBroker: MessageBroker[F]
)
