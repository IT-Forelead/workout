package com.itforelead.workout.config

case class AppConfig(
  jwtConfig: JwtConfig,
  dbConfig: DBConfig,
  redis: RedisConfig,
  serverConfig: HttpServerConfig,
  logConfig: LogConfig,
  messageBroker: BrokerConfig
)
