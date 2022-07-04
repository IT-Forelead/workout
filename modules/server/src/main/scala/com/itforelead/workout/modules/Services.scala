package com.itforelead.workout.modules

import cats.effect.Resource
import cats.effect.kernel.Async
import com.itforelead.workout.config.{BrokerConfig, SchedulerConfig}
import com.itforelead.workout.effects.{Background, GenUUID}
import com.itforelead.workout.services._
import com.itforelead.workout.services.redis.RedisClient
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import skunk.Session

object Services {
  def apply[F[_]: Async: GenUUID: Logger: Background](
    brokerConfig: BrokerConfig,
    schedulerConfig: SchedulerConfig,
    httpClient: Client[F],
    redisClient: RedisClient[F]
  )(implicit session: Resource[F, Session[F]]): Services[F] = {
    val messageBroker = MessageBroker[F](httpClient, brokerConfig)
    val members       = Members[F]
    val userSetting   = UserSettings[F]
    val payments      = Payments[F](userSetting)
    val messages      = Messages[F]

    new Services[F](
      users = Users[F],
      members = members,
      userValidation = Validations[F](messageBroker, members, redisClient),
      payments = payments,
      arrivalService = ArrivalService[F],
      messages = messages,
      userSettings = userSetting,
      messageBroker = messageBroker,
      notificationMessage = NotificationMessage.make[F](payments, messages, messageBroker, schedulerConfig)
    )
  }
}

final class Services[F[_]] private (
  val users: Users[F],
  val members: Members[F],
  val userValidation: Validations[F],
  val payments: Payments[F],
  val arrivalService: ArrivalService[F],
  val messages: Messages[F],
  val userSettings: UserSettings[F],
  val messageBroker: MessageBroker[F],
  val notificationMessage: NotificationMessage[F]
)
