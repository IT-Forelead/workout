package com.itforelead.workout.modules

import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import com.itforelead.workout.config.LogConfig
import com.itforelead.workout.domain.User
import com.itforelead.workout.implicits.CirceDecoderOps
import com.itforelead.workout.routes._
import com.itforelead.workout.services.redis.RedisClient
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.JwtToken
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._
import org.typelevel.log4cats.Logger
import pdi.jwt.JwtClaim

import scala.concurrent.duration.DurationInt

object HttpApi {
  def apply[F[_]: Async: Logger](
    security: Security[F],
    services: Services[F],
    redis: RedisClient[F],
    logConfig: LogConfig
  )(implicit F: Sync[F]): HttpApi[F] =
    new HttpApi[F](security, services, redis, logConfig)
}

final class HttpApi[F[_]: Async: Logger] private (
  security: Security[F],
  services: Services[F],
  redis: RedisClient[F],
  logConfig: LogConfig
) {
  private[this] val baseURL: String = "/"

  def findUser(token: JwtToken): JwtClaim => F[Option[User]] = _ =>
    OptionT(redis.get(token.value))
      .map(_.as[User])
      .value

  private[this] val usersMiddleware =
    JwtAuthMiddleware[F, User](security.userJwtAuth.value, findUser)

  // Auth routes
  private[this] val authRoutes = AuthRoutes[F](security.auth).routes(usersMiddleware)
  private[this] val userRoutes = new UserRoutes[F].routes(usersMiddleware)

  // Open routes
  private[this] val openRoutes: HttpRoutes[F] =
    userRoutes <+> authRoutes

  private[this] val routes: HttpRoutes[F] = Router(
    baseURL -> openRoutes
  )

  private[this] val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    } andThen { http: HttpRoutes[F] =>
      CORS.policy.withAllowOriginAll
        .withAllowCredentials(false)
        .apply(http)
    } andThen { http: HttpRoutes[F] =>
      Timeout(60.seconds)(http)
    }
  }

  private[this] val loggers: HttpApp[F] => HttpApp[F] = {
    { http: HttpApp[F] =>
      RequestLogger.httpApp(logConfig.httpHeader, logConfig.httpBody)(http)
    } andThen { http: HttpApp[F] =>
      ResponseLogger.httpApp(logConfig.httpHeader, logConfig.httpBody)(http)
    }
  }

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)

}