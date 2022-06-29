package com.itforelead.workout.modules

import cats.effect.Resource
import cats.effect.kernel.Async
import com.itforelead.workout.config.BrokerConfig
import com.itforelead.workout.effects.GenUUID
import com.itforelead.workout.services._
import com.itforelead.workout.services.redis.RedisClient
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import skunk.Session

object Services {
  def apply[F[_]: Async: GenUUID: Logger](
    brokerConfig: BrokerConfig,
    httpClient: Client[F],
    redisClient: RedisClient[F]
  )(implicit session: Resource[F, Session[F]]): Services[F] = {
    val messageBroker = MessageBroker[F](httpClient, brokerConfig)
    new Services[F](
      users = Users[F],
      members = Members[F],
      userValidation = UserValidation[F](messageBroker, redisClient),
      payments = Payments[F],
      messageBroker = messageBroker
    )
  }
}

final class Services[F[_]] private (
  val users: Users[F],
  val members: Members[F],
  val userValidation: UserValidation[F],
  val payments: Payments[F],
  val messageBroker: MessageBroker[F]
)
