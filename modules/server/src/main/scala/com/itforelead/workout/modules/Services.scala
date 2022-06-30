package com.itforelead.workout.modules

import cats.effect.Resource
import cats.effect.kernel.Async
import com.itforelead.workout.config.BrokerConfig
import com.itforelead.workout.domain.UserSetting
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
    val members       = Members[F]
    new Services[F](
      users = Users[F],
      members = members,
      userValidation = Validations[F](messageBroker, members, redisClient),
      payments = Payments[F],
      arrivalService = ArrivalService[F],
      userSettings = UserSettings[F],
      messageBroker = messageBroker
    )
  }
}

final class Services[F[_]] private (
  val users: Users[F],
  val members: Members[F],
  val userValidation: Validations[F],
  val payments: Payments[F],
  val arrivalService: ArrivalService[F],
  val userSettings: UserSettings[F],
  val messageBroker: MessageBroker[F]
)
