package com.itforelead.workout.config
import com.itforelead.workout.domain.AppEnv

case class AppConfig(
  env: AppEnv,
  jwtConfig: JwtConfig,
  dbConfig: DBConfig,
  redis: RedisConfig,
  serverConfig: HttpServerConfig,
  logConfig: LogConfig
)
