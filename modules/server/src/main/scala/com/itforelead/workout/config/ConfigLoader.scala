package com.itforelead.workout.config

import cats.effect.Async
import cats.implicits._
import ciris._
import ciris.refined.refTypeConfigDecoder
import com.itforelead.workout.domain.AppEnv
import com.itforelead.workout.domain.custom.refinements.{
  BucketName,
  EmailAddress,
  UriAddress,
  UrlAddress,
}
import com.itforelead.workout.types._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.net.{ NonSystemPortNumber, SystemPortNumber, UserPortNumber }
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.Uri

import java.time.LocalTime
import scala.concurrent.duration.FiniteDuration

object ConfigLoader {
  def databaseConfig: ConfigValue[Effect, DBConfig] = (
    env("POSTGRES_HOST").as[NonEmptyString],
    env("POSTGRES_PORT").as[NonSystemPortNumber],
    env("POSTGRES_USER").as[NonEmptyString],
    env("POSTGRES_PASSWORD").as[NonEmptyString].secret,
    env("POSTGRES_DATABASE").as[NonEmptyString],
    env("POSTGRES_POOL_SIZE").as[PosInt],
  ).parMapN(DBConfig.apply)

  def httpLogConfig: ConfigValue[Effect, LogConfig] = (
    env("HTTP_HEADER_LOG").as[Boolean],
    env("HTTP_BODY_LOG").as[Boolean],
  ).parMapN(LogConfig.apply)

  def httpServerConfig: ConfigValue[Effect, HttpServerConfig] = (
    env("HTTP_HOST").as[NonEmptyString],
    env("HTTP_PORT").as[UserPortNumber],
  ).parMapN(HttpServerConfig.apply)

  def messageBroker: ConfigValue[Effect, BrokerConfig] = (
    env("MESSAGE_BROKER_API").as[Uri],
    env("MESSAGE_BROKER_USERNAME").as[NonEmptyString],
    env("MESSAGE_BROKER_PASSWORD").as[NonEmptyString].secret,
    env("MESSAGE_BROKER_ENABLED").as[Boolean],
  ).parMapN(BrokerConfig.apply)

  def awsConfig: ConfigValue[Effect, AWSConfig] = (
    env("AWS_ACCESS_KEY").as[NonEmptyString],
    env("AWS_SECRET_KEY").as[NonEmptyString],
    env("AWS_ENDPOINT").as[UrlAddress],
    env("AWS_SIGNING_REGION").as[NonEmptyString],
    env("AWS_BUCKET_NAME").as[BucketName],
  ).parMapN(AWSConfig.apply)

  def redisConfig: ConfigValue[Effect, RedisConfig] =
    env("REDIS_SERVER_URI").as[UriAddress].map(RedisConfig.apply)

  def jwtConfig: ConfigValue[Effect, JwtConfig] = (
    env("ACCESS_TOKEN_SECRET_KEY").as[JwtAccessTokenKeyConfig].secret,
    env("PASSWORD_SALT").as[PasswordSalt].secret,
    env("JWT_TOKEN_EXPIRATION").as[FiniteDuration].map(TokenExpiration.apply),
  ).parMapN(JwtConfig.apply)

  def scheduler: ConfigValue[Effect, SchedulerConfig] = (
    env("SCHEDULER_START_TIME").as[LocalTime],
    env("SCHEDULER_PERIOD").as[FiniteDuration],
  ).parMapN(SchedulerConfig.apply)

  def monitoringConfig[F[_]: Async]: F[MonitoringConfig] = (
    env("MONITORING_HOST").as[NonEmptyString],
    env("MONITORING_PORT").as[SystemPortNumber],
    env("MONITORING_USERNAME").as[EmailAddress],
    env("MONITORING_PASSWORD").as[NonEmptyString],
    env("MONITORING_RECIPIENTS").as[List[EmailAddress]],
  ).parMapN(MonitoringConfig.apply).load[F]

  private[this] def mailerConfig: ConfigValue[Effect, MailerConfig] = (
    env("MAILER_HOST").as[NonEmptyString],
    env("MAILER_PORT").as[SystemPortNumber],
    env("MAILER_USERNAME").as[EmailAddress],
    env("MAILER_PASSWORD").as[NonEmptyString].secret,
  ).parMapN(MailerConfig.apply)

  def load[F[_]: Async]: F[AppConfig] = (
    env("APP_ENV").as[AppEnv],
    jwtConfig,
    databaseConfig,
    redisConfig,
    httpServerConfig,
    httpLogConfig,
    messageBroker,
    scheduler,
    awsConfig,
    mailerConfig,
  ).parMapN(AppConfig.apply).load[F]
}
