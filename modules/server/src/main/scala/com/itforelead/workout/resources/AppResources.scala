package com.itforelead.workout.resources

import cats.effect.std.Console
import cats.effect.{Async, Concurrent, Resource}
import cats.syntax.all._
import com.itforelead.workout.services.redis.RedisClient
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import eu.timepit.refined.auto._
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import org.typelevel.log4cats.Logger
import skunk._
import skunk.codec.text._
import skunk.implicits._
import skunk.util.Typer
import com.itforelead.workout.config.{AppConfig, DBConfig, RedisConfig}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

case class AppResources[F[_]](
  postgres: Resource[F, Session[F]],
  redis: RedisClient[F],
  httpClient: Client[F]
)

object AppResources {

  private[this] def checkRedisConnection[F[_]: Concurrent: Logger](
    redis: RedisCommands[F, String, String]
  ): F[Unit] =
    redis.info.flatMap {
      _.get("redis_version").traverse_ { v =>
        Logger[F].info(s"Connected to Redis $v")
      }
    }

  private[this] def checkPostgresConnection[F[_]: Concurrent: Logger](
    postgres: Resource[F, Session[F]]
  ): F[Unit] =
    postgres.use { session =>
      session.unique(sql"select version();".query(text)).flatMap { v =>
        Logger[F].info(s"Connected to Postgres $v")
      }
    }

  private[this] def redisResource[F[_]: Concurrent: Logger: MkRedis](c: RedisConfig): Resource[F, RedisClient[F]] =
    Redis[F].utf8(c.uri.value).evalTap(checkRedisConnection[F]).map(RedisClient[F])

  private[this] def postgresSqlResource[F[_]: Concurrent: Logger: Network: Console](c: DBConfig): SessionPool[F] =
    Session
      .pooled[F](
        host = c.host,
        port = c.port,
        user = c.user,
        password = Some(c.password.value),
        database = c.database,
        max = c.poolSize,
        strategy = Typer.Strategy.SearchPath
      )
      .evalTap(checkPostgresConnection[F])

  private def httpClient[F[_]: Async]: Resource[F, Client[F]] =
    EmberClientBuilder.default[F].build

  def apply[F[_]: Async: Console: Logger: MkRedis: Network](
    cfg: AppConfig
  ): Resource[F, AppResources[F]] =
    (
      postgresSqlResource(cfg.dbConfig),
      redisResource(cfg.redis),
      httpClient
    ).parMapN(AppResources[F])

}
