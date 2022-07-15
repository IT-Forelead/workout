package workout.stub_services

import cats.effect.Sync
import com.itforelead.workout.security.{JwtExpire, Tokens}
import com.itforelead.workout.services.redis.RedisClient
import com.itforelead.workout.services.{Auth, Users}
import workout.config.jwtConfig

object AuthMock {
  def tokens[F[_]: Sync]: Tokens[F] =
    Tokens.make[F](JwtExpire[F], jwtConfig.tokenConfig.value, jwtConfig.tokenExpiration)

  def apply[F[_]: Sync](users: Users[F], redis: RedisClient[F]): Auth[F] =
    Auth[F](jwtConfig.tokenExpiration, tokens, users, redis)
}
